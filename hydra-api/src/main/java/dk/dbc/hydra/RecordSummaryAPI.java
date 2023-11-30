package dk.dbc.hydra;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.hydra.dao.RawRepoConnector;
import dk.dbc.hydra.stats.RecordSummary;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

@Interceptors(StopwatchInterceptor.class)
@Stateless
@Path("/recordSummary")
public class RecordSummaryAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(RecordSummaryAPI.class);

    @EJB
    RawRepoConnector rawrepo;

    private final JSONBContext jsonbContext = new JSONBContext();

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/list")
    public Response getRecordsSummary() {
        LOGGER.entry();
        String res = "";

        try {
            final List<RecordSummary> providers = rawrepo.getRecordsSummary();

            res = jsonbContext.marshall(providers);

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | JSONBException ex) {
            LOGGER.error("Exception during getRecordsSummary", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

    @Stopwatch
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/refresh/{agencyId}")
    public Response refreshRecordsSummaryByAgencyId(@PathParam("agencyId") int agencyId) {
        LOGGER.entry();
        String res = "";

        try {
            rawrepo.refreshRecordSummaryByAgencyId(agencyId);

            return Response.ok().build();
        } catch (SQLException ex) {
            LOGGER.error("Exception during getRecordsSummary", ex);
            return Response.serverError().build();
        } finally {
            LOGGER.exit(res);
        }
    }

}
