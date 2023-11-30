/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.dao;

import dk.dbc.hydra.errors.WorkerErrorTypes;
import dk.dbc.hydra.errors.WorkerErrors;
import dk.dbc.hydra.queue.QueueException;
import dk.dbc.hydra.queue.QueueProvider;
import dk.dbc.hydra.queue.QueueWorker;
import dk.dbc.hydra.stats.QueueStats;
import dk.dbc.hydra.stats.RecordSummary;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import dk.dbc.rawrepo.RecordId;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Interceptors(StopwatchInterceptor.class)
@Stateless
public class RawRepoConnector {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(RawRepoConnector.class);

    private static final String SELECT_QUEUERULES_ALL = "SELECT * FROM queuerules";
    private static final String CALL_ENQUEUE_BULK = "SELECT * FROM enqueue_bulk(?, ?, ?, ?, ?)";
    private static final String SELECT_RECORDS_SUMMARY_ALL = "SELECT * FROM records_summary ORDER BY agencyid";
    private static final String SELECT_QUEUE_COUNT_BY_WORKER = "SELECT worker AS text, COUNT(*), MAX(queued) FROM queue GROUP BY worker ORDER BY worker";
    private static final String SELECT_QUEUE_COUNT_BY_AGENCY = "SELECT agencyid AS text, COUNT(*), MAX(queued) FROM queue GROUP BY agencyid ORDER BY agencyid";
    private static final String SELECT_ERRORS_COUNT_BY_WORKER = "SELECT worker, COUNT(*), MAX(queued) FROM jobdiag WHERE queued > now() - INTERVAL '30 DAYS' GROUP BY worker ORDER BY worker";
    private static final String SELECT_ERRORS_COUNT_BY_TYPE = "SELECT worker, error, COUNT(*), MAX(queued) FROM jobdiag WHERE queued > now() - INTERVAL '30 DAYS' GROUP BY worker, error ORDER BY MAX(queued) DESC LIMIT 1000";
    private static final String INSERT_INTO_QUEUE_FROM_JOBDIAG_BY_WORKER_AND_QUEUED =
            "INSERT INTO queue(bibliographicrecordid, agencyid, worker, priority) " +
                    "SELECT bibliographicrecordid, agencyid, worker, priority FROM jobdiag " +
                    "WHERE worker = ? AND queued <= ?";
    private static final String DELETE_FROM_JOBDIAG_BY_WORKER_AND_QUEUED =
            "DELETE FROM jobdiag WHERE worker = ? AND queued <= ?";
    private static final String CALL_REFRESH_RECORDS_SUMMARY_BY_AGENCY_ID = "SELECT * FROM refresh_records_summary_by_agencyid(?)";

    private static final DateTimeFormatter DATE_TIME_WITH_TIMEZONE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSX");

    @Resource(lookup = "jdbc/rawrepo")
    private DataSource globalDataSource;

    @PostConstruct
    public void postConstruct() {
        LOGGER.entry();

        if (!healthCheck()) {
            throw new RuntimeException("Unable to connect to RawRepo"); // Can't throw checked exceptions from postConstruct
        }
    }

    public boolean healthCheck() {
        try (Connection connection = globalDataSource.getConnection()) {
            try (CallableStatement stmt = connection.prepareCall("SELECT 1")) {
                try (ResultSet resultSet = stmt.executeQuery()) {
                    resultSet.next();

                    return true;
                }
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    @Stopwatch
    public List<QueueProvider> getProviders() throws SQLException {
        LOGGER.entry();
        HashMap<String, QueueProvider> providerMap = new HashMap<>();
        List<QueueProvider> result = new ArrayList<>();

        try (Connection connection = globalDataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(SELECT_QUEUERULES_ALL)) {
            try (ResultSet resultSet = stmt.executeQuery()) {
                QueueProvider provider;
                while (resultSet.next()) {
                    if (!providerMap.containsKey(resultSet.getString("provider"))) {
                        provider = new QueueProvider(resultSet.getString("provider"));
                        providerMap.put(provider.getName(), provider);
                    }

                    provider = providerMap.get(resultSet.getString("provider"));

                    QueueWorker worker = new QueueWorker(resultSet.getString("worker"),
                            resultSet.getString("changed").toUpperCase(),
                            resultSet.getString("leaf").toUpperCase());

                    provider.getWorkers().add(worker);
                }
            }

            // Convert from HashMap to flat list with only the values from the map
            for (String key : providerMap.keySet()) {
                result.add(providerMap.get(key));
            }

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    @Stopwatch
    public Set<RecordId> getEnrichmentForAgencies(Set<Integer> agencies, Boolean includeDeleted) throws SQLException {
        LOGGER.entry(agencies, includeDeleted);
        Set<RecordId> result = new HashSet<>();

        // There is no smart or elegant way of doing a select 'in' clause, so this will have to do
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT bibliographicrecordid, agencyid FROM records WHERE agencyid IN (");
        for (int i = 0; i < agencies.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        // By default deleted records are included with select *, so exclude them if they are not wanted
        if (!includeDeleted) {
            sb.append(" AND deleted = false");
        }
        sb.append(" AND mimetype = 'text/enrichment+marcxchange'");

        String statement = sb.toString();

        try (Connection connection = globalDataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(statement)) {
            int i = 1;
            for (Integer agencyId : agencies) {
                stmt.setInt(i++, agencyId);
            }

            LOGGER.debug("Executing statement: {}", statement);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    final String bibliographicRecordId = resultSet.getString("bibliographicrecordid");
                    final Integer agencyId = resultSet.getInt("agencyid");

                    result.add(new RecordId(bibliographicRecordId, agencyId));
                }
            }

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    @Stopwatch
    public Set<RecordId> getRecordsForAgencies(Set<Integer> agencies, boolean includeDeleted) throws SQLException {
        LOGGER.entry(agencies, includeDeleted);
        Set<RecordId> result = new HashSet<>();

        // There is no smart or elegant way of doing a select 'in' clause, so this will have to do
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT bibliographicrecordid, agencyid FROM records WHERE agencyid IN (");
        for (int i = 0; i < agencies.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        // By default deleted records are included with select *, so exclude them if they are not wanted
        if (!includeDeleted) {
            sb.append(" AND deleted = false");
        }

        String statement = sb.toString();

        try (Connection connection = globalDataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(statement)) {
            int i = 1;
            for (Integer agencyId : agencies) {
                stmt.setInt(i++, agencyId);
            }

            LOGGER.debug("Executing statement: {}", statement);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    final String bibliographicRecordId = resultSet.getString("bibliographicrecordid");
                    final Integer agencyId = resultSet.getInt("agencyid");

                    result.add(new RecordId(bibliographicRecordId, agencyId));
                }
            }

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    @Stopwatch
    public Set<RecordId> getRecordsForDBC(Set<Integer> agencies, boolean includeDeleted) throws SQLException {
        LOGGER.entry(agencies, includeDeleted);
        Set<RecordId> result = new HashSet<>();

        // There is no smart or elegant way of doing a select 'in' clause, so this will have to do
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT bibliographicrecordid, agencyid FROM records WHERE agencyid IN (");
        for (int i = 0; i < agencies.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        // By default deleted records are included with select *, so exclude them if they are not wanted
        if (!includeDeleted) {
            sb.append(" AND deleted = false");
        }

        String statement = sb.toString();

        try (Connection connection = globalDataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(statement)) {
            int i = 1;
            for (Integer agencyId : agencies) {
                stmt.setInt(i++, agencyId);
            }

            LOGGER.debug("Executing statement: {}", statement);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    final String bibliographicRecordId = resultSet.getString("bibliographicrecordid");
                    final Integer agencyId = resultSet.getInt("agencyid");

                    result.add(new RecordId(bibliographicRecordId, agencyId));
                    result.add(new RecordId(bibliographicRecordId, 191919));

                }
            }

            return result;
        } finally {
            // Not logging result as it may be millions of RecordIds
            LOGGER.exit();
        }
    }

    // Since a summary of the enqueuing have to be presented to the user we have to return the values somehow
    // This class only serves as a result container.
    public class EnqueueBulkResult {
        public String recordId;
        public Integer agencyId;
        public String worker;
        public Boolean queued;

        public EnqueueBulkResult(String recordId, Integer agencyId, String worker, Boolean queued) {
            this.recordId = recordId;
            this.agencyId = agencyId;
            this.worker = worker;
            this.queued = queued;
        }

        @Override
        public String toString() {
            return "EnqueueBulkResult{" +
                    "recordId='" + recordId + '\'' +
                    ", agencyId=" + agencyId +
                    ", worker='" + worker + '\'' +
                    ", queued=" + queued +
                    '}';
        }
    }

    @Stopwatch
    public List<EnqueueBulkResult> enqueueBulk(List<String> bibliographicRecordIdList,
                                               List<Integer> agencyList,
                                               List<String> providerList,
                                               List<Boolean> changedList,
                                               List<Boolean> leafList) throws SQLException, QueueException {
        LOGGER.entry();

        if (!(bibliographicRecordIdList.size() == agencyList.size() &&
                agencyList.size() == providerList.size() &&
                providerList.size() == changedList.size() &&
                changedList.size() == leafList.size())) {
            throw new QueueException("All input list must have same size");
        }

        // Convert true/false to Y/N
        List<String> changedListChar = new ArrayList<>();
        List<String> leafListChar = new ArrayList<>();

        for (Boolean changed : changedList) {
            changedListChar.add(changed ? "Y" : "N");
        }

        for (Boolean leaf : leafList) {
            leafListChar.add(leaf ? "Y" : "N");
        }

        List<EnqueueBulkResult> result = new ArrayList<>();
        try (Connection connection = globalDataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(CALL_ENQUEUE_BULK)) {
            stmt.setArray(1, stmt.getConnection().createArrayOf("VARCHAR", bibliographicRecordIdList.toArray()));
            stmt.setArray(2, stmt.getConnection().createArrayOf("NUMERIC", agencyList.toArray()));
            stmt.setArray(3, stmt.getConnection().createArrayOf("VARCHAR", providerList.toArray()));
            stmt.setArray(4, stmt.getConnection().createArrayOf("VARCHAR", changedListChar.toArray()));
            stmt.setArray(5, stmt.getConnection().createArrayOf("VARCHAR", leafListChar.toArray()));

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    final String recordId = resultSet.getString("bibliographicrecordid");
                    final Integer agencyId = resultSet.getInt("agencyid");
                    final String worker = resultSet.getString("worker");
                    final Boolean enqueued = resultSet.getString("queued").toUpperCase().equals("T");

                    result.add(new EnqueueBulkResult(recordId, agencyId, worker, enqueued));
                }
            }

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    public List<RecordSummary> getRecordsSummary() throws SQLException {
        LOGGER.entry();
        List<RecordSummary> result = new ArrayList<>();

        try (Connection connection = globalDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery(SELECT_RECORDS_SUMMARY_ALL)) {
                while (resultSet.next()) {
                    final int agencyId = resultSet.getInt("agencyid");
                    final int originalCount = resultSet.getInt("original_count");
                    final int enrichmentCount = resultSet.getInt("enrichment_count");
                    final int deletedCount = resultSet.getInt("deleted_count");
                    final Timestamp ajourDate = resultSet.getTimestamp("ajour_date");

                    result.add(new RecordSummary(agencyId,
                            originalCount,
                            enrichmentCount,
                            deletedCount,
                            ajourDate));
                }
            }

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    public void refreshRecordSummaryByAgencyId(int agencyId) throws SQLException {
        LOGGER.entry();

        try (Connection connection = globalDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(CALL_REFRESH_RECORDS_SUMMARY_BY_AGENCY_ID)) {
            stmt.setInt(1, agencyId);

            stmt.executeQuery();
        } finally {
            LOGGER.exit();
        }
    }

    public List<QueueStats> getQueueStatsByWorker() throws SQLException {
        LOGGER.entry();
        List<QueueStats> result = new ArrayList<>();

        try {
            return result = getQueueStats(SELECT_QUEUE_COUNT_BY_WORKER);
        } finally {
            LOGGER.exit(result);
        }
    }

    public List<QueueStats> getQueueStatsByAgency() throws SQLException {
        LOGGER.entry();
        List<QueueStats> result = new ArrayList<>();

        try {
            return result = getQueueStats(SELECT_QUEUE_COUNT_BY_AGENCY);
        } finally {
            LOGGER.exit(result);
        }
    }

    public List<WorkerErrors> getWorkerErrors() throws SQLException {
        LOGGER.entry();
        final List<WorkerErrors> result = new ArrayList<>();

        try (Connection connection = globalDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery(SELECT_ERRORS_COUNT_BY_WORKER)) {
                while (resultSet.next()) {
                    final String worker = resultSet.getString("worker");
                    final int count = resultSet.getInt("count");
                    final String date = resultSet.getString("max");
                    result.add(new WorkerErrors(worker, count, date));
                }
            }
            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    public List<WorkerErrorTypes> getWorkerErrorTypes() throws SQLException {
        LOGGER.entry();
        final List<WorkerErrorTypes> result = new ArrayList<>();

        try (Connection connection = globalDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery(SELECT_ERRORS_COUNT_BY_TYPE)) {
                while (resultSet.next()) {
                    final String worker = resultSet.getString("worker");
                    final String error = resultSet.getString("error");
                    final int count = resultSet.getInt("count");
                    final String date = resultSet.getString("max");
                    result.add(new WorkerErrorTypes(worker, error, count, date));
                }
            }
            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    public void reEnqueue(WorkerErrors workerErrors) throws SQLException {
        LOGGER.entry();
        final Timestamp timestamp = getTimestampFromDateTimeWithTimezone(workerErrors.getDate());
        try (Connection connection = globalDataSource.getConnection();
             PreparedStatement enqueueStmt = connection.prepareStatement(
                     INSERT_INTO_QUEUE_FROM_JOBDIAG_BY_WORKER_AND_QUEUED);
             PreparedStatement deleteStmt = connection.prepareStatement(
                     DELETE_FROM_JOBDIAG_BY_WORKER_AND_QUEUED)) {
            enqueueStmt.setString(1, workerErrors.getWorker());
            enqueueStmt.setTimestamp(2, timestamp);
            int rowsUpdated = enqueueStmt.executeUpdate();
            LOGGER.info("re-enqueued {} rows for {}", rowsUpdated, workerErrors.getWorker());
            deleteStmt.setString(1, workerErrors.getWorker());
            deleteStmt.setTimestamp(2, timestamp);
            rowsUpdated = deleteStmt.executeUpdate();
            LOGGER.info("deleted {} rows from jobdiag for {}", rowsUpdated, workerErrors.getWorker());
        } finally {
            LOGGER.exit();
        }
    }

    private List<QueueStats> getQueueStats(String queueQuery) throws SQLException {
        LOGGER.entry(queueQuery);
        List<QueueStats> result = new ArrayList<>();

        try (Connection connection = globalDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery(queueQuery)) {
                while (resultSet.next()) {
                    final String text = resultSet.getString("text");
                    final int count = resultSet.getInt("count");
                    final String date = resultSet.getString("max");

                    result.add(new QueueStats(text, count, date));
                }
            }

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    private static Timestamp getTimestampFromDateTimeWithTimezone(String dateTimeString) {
        final OffsetDateTime dateTime = OffsetDateTime.parse(dateTimeString, DATE_TIME_WITH_TIMEZONE_FORMATTER);
        return Timestamp.valueOf(dateTime.toLocalDateTime());  // to retain nanosecond precision
    }
}
