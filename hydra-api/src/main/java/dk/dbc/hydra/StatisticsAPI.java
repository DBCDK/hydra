/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.dao.RawRepoConnector;
import dk.dbc.hydra.stats.QueueStats;
import dk.dbc.hydra.stats.RecordStats;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

@Interceptors(StopwatchInterceptor.class)
@Stateless
@Path(ApplicationConstants.API_STATS)
public class StatisticsAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(StatisticsAPI.class);

    @EJB
    RawRepoConnector rawrepo;

    private final JSONBContext jsonbContext = new JSONBContext();

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_STATS_RECORDS)
    public Response getRecordStatsByAgency() {
        LOGGER.entry();
        String res = "";

        try {
            final List<RecordStats> providers = rawrepo.getStatsRecordByAgency();

            res = jsonbContext.marshall(providers);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getRecordStatsByAgency", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_STATS_QUEUE_WORKERS)
    public Response getQueueStatsByWorker() {
        LOGGER.entry();
        String res = "";

        try {
            final List<QueueStats> queueStats = rawrepo.getStatsQueueByWorker();

            res = jsonbContext.marshall(queueStats);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getQueueStatsByWorker", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_STATS_QUEUE_AGENCIES)
    public Response getQueueStatsByAgency() {
        LOGGER.entry();
        String res = "";

        try {
            final List<QueueStats> queueStats = rawrepo.getStatsQueueByAgency();

            res = jsonbContext.marshall(queueStats);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getQueueStatsByAgency", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_STATS_QUEUE_ERRORS)
    public Response getQueueStatsByError() {
        LOGGER.entry();
        String res = "";

        try {
            final List<QueueStats> queueStats = rawrepo.getStatsQueueByError();

            res = jsonbContext.marshall(queueStats);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getQueueStatsByError", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

}