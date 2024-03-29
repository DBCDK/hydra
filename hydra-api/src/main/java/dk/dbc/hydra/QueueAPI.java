/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.dao.HoldingsItemsConnector;
import dk.dbc.hydra.dao.RawRepoConnector;
import dk.dbc.hydra.dao.VipCoreConnector;
import dk.dbc.hydra.queue.AgencyAnalysis;
import dk.dbc.hydra.queue.EnqueueAgencyRequest;
import dk.dbc.hydra.queue.EnqueueAgencyResponse;
import dk.dbc.hydra.queue.EnqueueRecordsRequest;
import dk.dbc.hydra.queue.EnqueueRecordsResponse;
import dk.dbc.hydra.queue.QueueException;
import dk.dbc.hydra.queue.QueueJob;
import dk.dbc.hydra.queue.QueueProcessRequest;
import dk.dbc.hydra.queue.QueueProcessResponse;
import dk.dbc.hydra.queue.QueueProvider;
import dk.dbc.hydra.queue.QueueType;
import dk.dbc.hydra.queue.QueueValidateRequest;
import dk.dbc.hydra.queue.QueueValidateResponse;
import dk.dbc.hydra.queue.RecordEnqueueResult;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.dto.EnqueueAgencyResponseDTO;
import dk.dbc.rawrepo.dto.EnqueueResultCollectionDTO;
import dk.dbc.rawrepo.dto.EnqueueResultDTO;
import dk.dbc.rawrepo.dto.QueueWorkerCollectionDTO;
import dk.dbc.rawrepo.queue.QueueServiceConnector;
import dk.dbc.rawrepo.queue.QueueServiceConnectorException;
import dk.dbc.vipcore.exception.VipCoreException;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Interceptors(StopwatchInterceptor.class)
@Stateless
@Path(ApplicationConstants.API_QUEUE)
public class QueueAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(QueueAPI.class);

    private static final String MESSAGE_FAIL_NO_RECORDS = "Der blev ikke fundet nogen poster, så intet kan lægges på kø";
    private static final String MESSAGE_FAIL_INVALID_AGENCY_FORMAT = "Værdien '%s' har ikke et gyldigt format for et biblioteksnummer. Formatet skal være seks tal";
    private static final String MESSAGE_FAIL_INVAILD_AGENCY_ID = "Biblioteksnummeret '%s' tilhører ikke en af biblioteksgrupperne %s";
    private static final String MESSAGE_FAIL_INVALID_RECORD_ID_FORMAT = "Værdien '%s' er ikke gyldigt format. Formatet skal være <tekst>:<seks tal>.";
    private static final String MESSAGE_FAIL_QUEUETYPE_NULL = "Der skal angives en køtype";
    private static final String MESSAGE_FAIL_QUEUETYPE = "Køtypen '%s' kunne ikke valideres";
    private static final String MESSAGE_FAIL_PROVIDER_NULL = "Der skal angives en provider";
    private static final String MESSAGE_FAIL_PROVIDER = "Provideren '%s' kunne ikke valideres";
    private static final String MESSAGE_FAIL_WORKER_MISSING = "Der skal angives en worker";
    private static final String MESSAGE_FAIL_PROVIDER_MISSING = "Der skal angives en provider";
    private static final String MESSAGE_FAIL_AGENCY_MISSING = "Der skal angives mindst ét biblioteksnummer";
    private static final String MESSAGE_FAIL_RECORD_ID_MISSING = "Der skal angives mindst ét post id";

    private static final String MESSAGE_FAIL_SESSION_ID_NULL = "Der skal være angivet et sessionId";
    private static final String MESSAGE_FAIL_SESSION_ID_NOT_FOUND = "SessionId '%s' blev ikke fundet";
    private static final String MESSAGE_FAIL_CHUNK_TOO_BIG = "Chunk index '%s' er for stort";
    private static final String MESSAGE_FAIL_CHUNK_NEGATIVE = "Chunk index må ikke være negativt";

    private static final Pattern PATTERN_RECORD_ID = Pattern.compile("\\w+:\\d{6}"); // matches <something><colon><six digits>
    private static final Pattern PATTERN_AGENCY_ID = Pattern.compile("(\\d{6})"); // Digit, length of 6

    private static final Integer CHUNK_SIZE = 5000;

    @EJB
    VipCoreConnector vipCoreConnector;

    @EJB
    RawRepoConnector rawrepo;

    @EJB
    HoldingsItemsConnector holdingsItemsConnector;

    @Inject
    QueueServiceConnector queueServiceConnector;

    @Inject
    @ConfigProperty(name = "INSTANCE_NAME", defaultValue = "dev")
    String INSTANCE_NAME;

    private final JSONBContext jsonbContext = new JSONBContext();

    // Not made private to facilitate easier testing
    final PassiveExpiringMap<String, QueueJob> jobCache = new PassiveExpiringMap<>(1000 * 60 * 60 * 8); // 8 hours

    @Stopwatch
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("validate")
    public Response validate(String inputStr) {
        String res = "";
        final QueueValidateResponse response = new QueueValidateResponse();
        final String sessionId = UUID.randomUUID().toString();

        try {
            try {
                final QueueValidateRequest queueValidateRequest = jsonbContext.unmarshall(inputStr, QueueValidateRequest.class);

                LOGGER.debug("Validate request with sessionId {}: {}", sessionId, queueValidateRequest);

                final QueueJob queueJob = prepareQueueJob(queueValidateRequest);
                loadRecordIdsForQueuing(queueJob);

                if (queueJob.getRecordIdList().isEmpty()) {
                    throw new QueueException(MESSAGE_FAIL_NO_RECORDS);
                }

                final Map<Integer, Integer> agencySummary = new HashMap<>();

                for (RecordId recordId : queueJob.getRecordIdList()) {
                    Integer agencyId = recordId.getAgencyId();
                    Integer count = 1;

                    if (agencySummary.containsKey(agencyId)) {
                        count = agencySummary.get(agencyId);
                        count++;
                    }

                    agencySummary.put(agencyId, count);
                }

                this.jobCache.put(sessionId, queueJob);

                agencySummary.forEach((key, value) -> response.getAgencyAnalysisList().add(new AgencyAnalysis(key, value)));

                response.setChunks(queueJob.getRecordIdList().size() / CHUNK_SIZE);
                response.setSessionId(sessionId);
                response.setValidated(true);

                res = jsonbContext.marshall(response);
            } catch (QueueException e) {
                response.setValidated(false);
                response.setMessage(e.getMessage());

                res = jsonbContext.marshall(response);
            }
            LOGGER.debug(response.toString());

            return Response.ok(res).build();
        } catch (SQLException | JSONBException | HoldingsItemsException ex) {
            LOGGER.error("Unexpected exception:", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

    private void loadRecordIdsForQueuing(QueueJob queueJob) throws SQLException, HoldingsItemsException {
        Set<RecordId> holdingsItemsRecordIds;
        Set<RecordId> fbsRecordIds;
        Set<RecordId> recordMap = null;
        final QueueType queueType = queueJob.getQueueType();
        final Set<Integer> agencyList = queueJob.getAgencyIdList();
        final boolean includeDeleted = queueJob.isIncludeDeleted();

        switch (queueType.getKey()) {
            case QueueType.DBC_COMMON_ONLY:
                recordMap = rawrepo.getRecordsForDBC(agencyList, includeDeleted);
                break;
            case QueueType.FFU: // Intended fall through
            case QueueType.FBS_LOCAL:
                recordMap = rawrepo.getRecordsForAgencies(agencyList, includeDeleted);
                break;
            case QueueType.FBS_ENRICHMENT:
                recordMap = rawrepo.getEnrichmentForAgencies(agencyList, includeDeleted);
                break;
            case QueueType.FBS_HOLDINGS:
                holdingsItemsRecordIds = holdingsItemsConnector.getHoldingsRecords(agencyList);
                fbsRecordIds = rawrepo.getRecordsForAgencies(agencyList, includeDeleted);
                recordMap = convertNotExistingRecordIds(holdingsItemsRecordIds, fbsRecordIds);
                break;
            case QueueType.FBS_EVERYTHING:
                holdingsItemsRecordIds = holdingsItemsConnector.getHoldingsRecords(agencyList);
                fbsRecordIds = rawrepo.getRecordsForAgencies(agencyList, includeDeleted);
                recordMap = convertNotExistingRecordIds(holdingsItemsRecordIds, fbsRecordIds);
                // Rawrepo might contain records which the library doesn't have holdings on, so we have to add
                // the FBS records to the list as well
                recordMap.addAll(fbsRecordIds);
                break;
            case QueueType.IMS:
                recordMap = holdingsItemsConnector.getHoldingsRecords(agencyList);
                recordMap.addAll(rawrepo.getRecordsForAgencies(agencyList, includeDeleted));
                break;
        }

        queueJob.setRecordIdList(recordMap);
    }

    @Stopwatch
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("process")
    public Response process(String inputStr) {
        String res = "";
        final QueueProcessResponse response = new QueueProcessResponse();

        try {
            try {
                final QueueProcessRequest queueProcessRequest = jsonbContext.unmarshall(inputStr, QueueProcessRequest.class);

                LOGGER.debug(queueProcessRequest.toString());

                if (queueProcessRequest.getSessionId() == null || "null".equals(queueProcessRequest.getSessionId())) {
                    throw new QueueException(MESSAGE_FAIL_SESSION_ID_NULL);
                }

                if (jobCache.get(queueProcessRequest.getSessionId()) == null) {
                    throw new QueueException(String.format(MESSAGE_FAIL_SESSION_ID_NOT_FOUND, queueProcessRequest.getSessionId()));
                }

                final QueueJob queueJob = jobCache.get(queueProcessRequest.getSessionId());
                final int chunkIndex = queueProcessRequest.getChunkIndex();
                final String provider = queueJob.getProvider();
                final QueueType queueType = queueJob.getQueueType();
                final List<RecordId> recordIdList = new ArrayList<>(queueJob.getRecordIdList());

                if (chunkIndex > queueJob.getRecordIdList().size() / CHUNK_SIZE) {
                    throw new QueueException(String.format(MESSAGE_FAIL_CHUNK_TOO_BIG, chunkIndex));
                }

                if (chunkIndex < 0) {
                    throw new QueueException(MESSAGE_FAIL_CHUNK_NEGATIVE);
                }

                final int jobChunkStart = Math.max(0, chunkIndex * CHUNK_SIZE);
                final int jobChunkEnd = Math.min(recordIdList.size(), (chunkIndex + 1) * CHUNK_SIZE);
                final List<RecordId> chunk = recordIdList.subList(jobChunkStart, jobChunkEnd);

                LOGGER.info("Processing QueueJob with sessionId {} from {} to {} (total {} records)",
                        queueProcessRequest.getSessionId(),
                        jobChunkStart, jobChunkEnd,
                        queueJob.getRecordIdList().size());

                final List<String> bibliographicRecordIdList = new ArrayList<>();
                final List<Integer> agencyListBulk = new ArrayList<>();
                final List<String> providerList = new ArrayList<>();
                final List<Boolean> changedList = new ArrayList<>();
                final List<Boolean> leafList = new ArrayList<>();

                // This is a bit awkward construction, but necessary for setting parameters for a prepared statement
                for (RecordId recordId : chunk) {
                    bibliographicRecordIdList.add(recordId.getBibliographicRecordId());
                    agencyListBulk.add(recordId.getAgencyId());
                    providerList.add(provider);
                    changedList.add(queueType.isChanged());
                    leafList.add(queueType.isLeaf());
                }

                rawrepo.enqueueBulk(bibliographicRecordIdList, agencyListBulk, providerList, changedList, leafList);

                response.setValidated(true);

                res = jsonbContext.marshall(response);
            } catch (QueueException e) {
                response.setValidated(false);
                response.setMessage(e.getMessage());

                res = jsonbContext.marshall(response);
            }
            LOGGER.debug(response.toString());

            return Response.ok(res).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Unexpected exception:", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

    private QueueJob prepareQueueJob(QueueValidateRequest queueValidateRequest) throws QueueException {
        QueueJob queueJob = new QueueJob();

        try {
            final List<String> providerNames =
                    rawrepo.getProviders()
                            .stream()
                            .map(QueueProvider::getName)
                            .collect(Collectors.toList());

            // Convert list of QueueProvider to list of provider names
            final String provider = queueValidateRequest.getProvider();

            if (provider == null) {
                throw new QueueException(MESSAGE_FAIL_PROVIDER_NULL);
            }

            if (provider.isEmpty() || !providerNames.contains(provider)) {
                throw new QueueException(String.format(MESSAGE_FAIL_PROVIDER, provider));
            }
            queueJob.setProvider(provider);

            final String queueTypeString = queueValidateRequest.getQueueType();

            if (queueTypeString == null) {
                throw new QueueException(MESSAGE_FAIL_QUEUETYPE_NULL);
            }

            final QueueType queueType = QueueType.fromString(queueTypeString);
            if (queueType == null) {
                throw new QueueException(String.format(MESSAGE_FAIL_QUEUETYPE, queueValidateRequest.getQueueType()));
            }
            queueJob.setQueueType(queueType);

            queueJob.setIncludeDeleted(queueValidateRequest.isIncludeDeleted());

            String agencyString = queueValidateRequest.getAgencyText();
            if (agencyString == null) {
                throw new QueueException(MESSAGE_FAIL_AGENCY_MISSING);
            }
            // agencyText is a text field in which the user can write anything. Therefore we have to sanitize the
            // field first. Transform newline and space separation into comma separation for easier splitting
            agencyString = agencyString.replace("\n", ",");
            agencyString = agencyString.replace(" ", ",");
            // Remove eventual double or triple commas as a result of the replace
            while (agencyString.contains(",,")) {
                agencyString = agencyString.replace(",,", ",");
            }
            final List<String> agencies = Arrays.asList(agencyString.split(","));

            final Set<String> allowedAgencies = new HashSet<>();
            for (String catalogingTemplateSet : queueType.getCatalogingTemplateSets()) {
                allowedAgencies.addAll(vipCoreConnector.getLibrariesByCatalogingTemplateSet(catalogingTemplateSet));
            }

            agencies.forEach(s -> s = s.trim());

            if (agencies.isEmpty()) {
                throw new QueueException(MESSAGE_FAIL_AGENCY_MISSING);
            }

            final Set<Integer> agencyList = new HashSet<>();

            for (String agency : agencies) {
                // This filtering could be done more efficiently with removeIf, however we want to check the format
                // so the user can be informed of invalid format
                if (!PATTERN_AGENCY_ID.matcher(agency).find()) {
                    throw new QueueException(String.format(MESSAGE_FAIL_INVALID_AGENCY_FORMAT, agency));
                }
                if (!allowedAgencies.contains(agency)) {
                    throw new QueueException(String.format(MESSAGE_FAIL_INVAILD_AGENCY_ID, agency, queueType.getCatalogingTemplateSets()));
                }
                agencyList.add(Integer.parseInt(agency));
            }
            queueJob.setAgencyIdList(agencyList);

            return queueJob;
        } catch (SQLException | VipCoreException ex) {
            LOGGER.error("Exception during prepareQueueJob", ex);
            throw new QueueException("Exception during prepareQueueJob: " + ex.toString());
        } finally {
            LOGGER.exit(queueJob);
        }
    }

    /**
     * This function is necessary due to how Corepo works.
     * Corepo only contains real/existing record. However holdings items will often return ids of records
     * that doesn't actually exist. In those case we have to override the agencyId to be that of 191919 instead
     * so that Corepo receives the correct register
     *
     * @param holdingsItemsRecordIds Set of record ids from holdings items
     * @param fbsRecordIds           Set of record ids from rawrepo
     * @return Set of record ids
     */
    private Set<RecordId> convertNotExistingRecordIds(Set<RecordId> holdingsItemsRecordIds, Set<RecordId> fbsRecordIds) {
        final Set<RecordId> result = new HashSet<>();

        for (RecordId holdingsItemsRecordId : holdingsItemsRecordIds) {
            if (fbsRecordIds.contains(holdingsItemsRecordId)) {
                result.add(holdingsItemsRecordId);
            } else {
                result.add(new RecordId(holdingsItemsRecordId.getBibliographicRecordId(), 191919));
            }
        }

        return result;
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("providers")
    public Response getProviderNames() {
        LOGGER.entry();
        String res = "";

        try {
            final List<QueueProvider> providers = rawrepo.getProviders();
            LOGGER.debug(providers.toString());

            res = jsonbContext.marshall(providers);
            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getProviderInfo", ex);
            return Response.serverError().entity(ex.toString()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("types")
    public Response getQueueTypes() {
        LOGGER.entry();

        String res = "";
        try {
            final List<QueueType> queueTypes = new ArrayList<>();
            queueTypes.add(QueueType.ffu());
            queueTypes.add(QueueType.fbsRawrepo());
            queueTypes.add(QueueType.fbsRawrepoEnrichment());
            queueTypes.add(QueueType.fbsHoldings());
            queueTypes.add(QueueType.fbsEverything());
            queueTypes.add(QueueType.ims());

            // Hack to only enable DBC queue type on basismig environment
            if (INSTANCE_NAME.toLowerCase().contains("basismig")) {
                queueTypes.add(QueueType.dbcCommon());
            }

            res = jsonbContext.marshall(queueTypes);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (JSONBException ex) {
            LOGGER.error("Unexpected exception:", ex);
            return Response.serverError().entity(ex.toString()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("workers")
    public Response getWorkers() {
        LOGGER.entry();

        String res = "";
        try {
            final QueueWorkerCollectionDTO queueWorkerCollectionDTO = queueServiceConnector.getQueueWorkers();

            res = jsonbContext.marshall(queueWorkerCollectionDTO.getWorkers());

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (JSONBException | QueueServiceConnectorException ex) {
            LOGGER.error("Unexpected exception:", ex);
            return Response.serverError().entity(ex.toString()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("enqueue/agency")
    public Response enqueueAgency(String inputStr) {
        LOGGER.entry();
        LOGGER.info(inputStr);

        final EnqueueAgencyResponse response = new EnqueueAgencyResponse();
        String res = "";

        try {
            try {
                final EnqueueAgencyRequest request = jsonbContext.unmarshall(inputStr, EnqueueAgencyRequest.class);
                final Set<Integer> agencyIds = new HashSet<>();

                if (request.getAgencies() == null || request.getAgencies().isEmpty()) {
                    throw new QueueException(MESSAGE_FAIL_AGENCY_MISSING);
                }

                // Clean and check agency values
                for (String agencyIdStr : request.getAgencies().split("\n")) {
                    agencyIdStr = agencyIdStr.trim();
                    if (!agencyIdStr.isEmpty()) {
                        if (!PATTERN_AGENCY_ID.matcher(agencyIdStr).find()) {
                            throw new QueueException(String.format(MESSAGE_FAIL_INVALID_AGENCY_FORMAT, agencyIdStr));
                        }

                        agencyIds.add(Integer.parseInt(agencyIdStr));
                    }
                }

                if (request.getWorker() == null || request.getWorker().isEmpty()) {
                    throw new QueueException(MESSAGE_FAIL_WORKER_MISSING);
                }

                response.setAgencyAnalysisList(new ArrayList<>());
                response.setValidated(true);

                for (Integer agencyId : agencyIds) {
                    final QueueServiceConnector.EnqueueParams params = new QueueServiceConnector.EnqueueParams();

                    if (request.getPriority() != null) {
                        params.withPriority(request.getPriority());
                    }
                    if (request.isEnqueueDBCAsEnrichment() && Arrays.asList(870970, 870971, 870974, 870976, 870979, 190002, 190004).contains(agencyId)) {
                        params.withEnqueueAs(191919);
                    }

                    final EnqueueAgencyResponseDTO responseDTO = queueServiceConnector.enqueueAgency(agencyId, request.getWorker(), params);
                    response.getAgencyAnalysisList().add(new AgencyAnalysis(agencyId, responseDTO.getCount()));
                }

                res = jsonbContext.marshall(response);

                return Response.ok(res, MediaType.APPLICATION_JSON).build();
            } catch (QueueException e) {
                response.setValidated(false);
                response.setMessage(e.getMessage());

                res = jsonbContext.marshall(response);

                return Response.ok(res, MediaType.APPLICATION_JSON).build();
            }
        } catch (JSONBException | QueueServiceConnectorException ex) {
            LOGGER.error("Unexpected exception:", ex);
            return Response.serverError().entity(ex.toString()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("enqueue/records")
    public Response enqueueRecords(String inputStr) {
        LOGGER.entry();
        LOGGER.info(inputStr);

        final EnqueueRecordsResponse response = new EnqueueRecordsResponse();
        String res = "";

        try {
            try {
                final EnqueueRecordsRequest request = jsonbContext.unmarshall(inputStr, EnqueueRecordsRequest.class);
                final Set<RecordId> recordIds = new HashSet<>();

                if (request.getRecordIds() == null || request.getRecordIds().isEmpty()) {
                    throw new QueueException(MESSAGE_FAIL_RECORD_ID_MISSING);
                }

                if (request.getProvider() == null || request.getProvider().isEmpty()) {
                    throw new QueueException(MESSAGE_FAIL_PROVIDER_MISSING);
                }

                // Clean and check agency values
                for (String recordIdStr : request.getRecordIds().split("\n")) {
                    recordIdStr = recordIdStr.trim();
                    if (!recordIdStr.isEmpty()) {
                        if (!PATTERN_RECORD_ID.matcher(recordIdStr).find()) {
                            throw new QueueException(String.format(MESSAGE_FAIL_INVALID_RECORD_ID_FORMAT, recordIdStr));
                        }

                        final String[] split = recordIdStr.split(":");
                        final String bibliographicRecordId = split[0].trim();
                        final int agencyId = Integer.parseInt(split[1].trim());

                        recordIds.add(new RecordId(bibliographicRecordId, agencyId));
                    }
                }

                response.setRecordEnqueueResultList(new ArrayList<>());
                response.setValidated(true);

                LOGGER.info("Fundne recordIds: {}", recordIds);

                for (RecordId recordId : recordIds) {
                    final QueueServiceConnector.EnqueueParams params = new QueueServiceConnector.EnqueueParams();

                    if (request.getPriority() != null) {
                        params.withPriority(request.getPriority());
                    }

                    if (request.getChanged() != null) {
                        params.withChanged(request.getChanged());
                    }

                    if (request.getLeaf() != null) {
                        params.withLeaf(request.getLeaf());
                    }

                    final EnqueueResultCollectionDTO responseDTO = queueServiceConnector.enqueueRecord(recordId.getAgencyId(), recordId.getBibliographicRecordId(), request.getProvider(), params);
                    for (EnqueueResultDTO enqueueResultDTO : responseDTO.getEnqueueResults()) {
                        response.getRecordEnqueueResultList().add(new RecordEnqueueResult(enqueueResultDTO.getBibliographicRecordId(), enqueueResultDTO.getAgencyId(), enqueueResultDTO.getWorker(), enqueueResultDTO.isQueued()));
                    }
                }

                res = jsonbContext.marshall(response);

                return Response.ok(res, MediaType.APPLICATION_JSON).build();
            } catch (QueueException e) {
                response.setValidated(false);
                response.setMessage(e.getMessage());

                res = jsonbContext.marshall(response);

                return Response.ok(res, MediaType.APPLICATION_JSON).build();
            }
        } catch (JSONBException | QueueServiceConnectorException ex) {
            LOGGER.error("Unexpected exception:", ex);
            return Response.serverError().entity(ex.toString()).build();
        } finally {
            LOGGER.exit(res);
        }
    }

}
