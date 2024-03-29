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
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    @Path(ApplicationConstants.API_STATS_QUEUE_WORKERS)
    public Response getQueueStatsByWorker() {
        LOGGER.entry();
        String res = "";

        try {
            final List<QueueStats> queueStats = rawrepo.getQueueStatsByWorker();

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
            final List<QueueStats> queueStats = rawrepo.getQueueStatsByAgency();

            res = jsonbContext.marshall(queueStats);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getQueueStatsByAgency", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }
}
