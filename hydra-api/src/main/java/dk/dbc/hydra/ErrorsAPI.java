/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.dao.RawRepoConnector;
import dk.dbc.hydra.errors.WorkerErrorTypes;
import dk.dbc.hydra.errors.WorkerErrors;
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
@Path(ApplicationConstants.API_ERRORS)
public class ErrorsAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ErrorsAPI.class);

    @EJB
    RawRepoConnector rawrepo;

    private final JSONBContext jsonbContext = new JSONBContext();

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_ERRORS_WORKERS)
    public Response getErrorsByWorker() {
        LOGGER.entry();
        String res = "";

        try {
            final List<WorkerErrors> workerErrorSummaries = rawrepo.getWorkerErrors();
            res = jsonbContext.marshall(workerErrorSummaries);
            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getErrorsByWorker", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_ERRORS_TYPE)
    public Response getErrorsByType() {
        LOGGER.entry();
        String res = "";

        try {
            final List<WorkerErrorTypes> workerErrorTypes = rawrepo.getWorkerErrorTypes();
            res = jsonbContext.marshall(workerErrorTypes);
            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getErrorsByType", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }
}
