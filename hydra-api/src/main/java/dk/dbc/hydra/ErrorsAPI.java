/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra;

import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.dao.RawRepoConnector;
import dk.dbc.hydra.errors.WorkerErrorTypes;
import dk.dbc.hydra.errors.WorkerErrors;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

@Interceptors(StopwatchInterceptor.class)
@Stateless
@Path(ApplicationConstants.API_ERRORS)
public class ErrorsAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ErrorsAPI.class);

    @EJB
    RawRepoConnector rawrepo;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_ERRORS_WORKERS)
    @Stopwatch
    public List<WorkerErrors> getErrorsByWorker() throws SQLException {
        LOGGER.entry();
        try {
            return rawrepo.getWorkerErrors();
        } finally {
            LOGGER.exit();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_ERRORS_WORKERS)
    @Stopwatch
    public void reEnqueueErrorsByWorker(WorkerErrors workerErrors) throws SQLException {
        LOGGER.entry();
        try {
            LOGGER.info("re-enqueueing errors for {}", workerErrors);
            rawrepo.reEnqueue(workerErrors);
        } finally {
            LOGGER.exit();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_ERRORS_TYPE)
    @Stopwatch
    public List<WorkerErrorTypes> getErrorsByType() throws SQLException {
        LOGGER.entry();
        try {
            return rawrepo.getWorkerErrorTypes();
        } finally {
            LOGGER.exit();
        }
    }
}
