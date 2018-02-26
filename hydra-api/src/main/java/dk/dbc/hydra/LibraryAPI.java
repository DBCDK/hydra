/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.dao.OpenAgencyConnector;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import dk.dbc.openagency.client.OpenAgencyException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Interceptors(StopwatchInterceptor.class)
@Stateless
@Path(ApplicationConstants.API_LIBRARY)
public class LibraryAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(LibraryAPI.class);

    @EJB
    private OpenAgencyConnector openAgency;

    private final JSONBContext mapper = new JSONBContext();

    @Stopwatch
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_LIBRARY_CATALOGING_TEMPLATE_SET + "/{template}")
    public Response getLibrariesByCatalogingTemplateSet(@PathParam("template") String template) {
        LOGGER.entry(template);
        String res = "";
        try {
            try {
                Set<String> libraries = openAgency.getLibrariesByCatalogingTemplateSet(template);

                res = mapper.marshall(libraries);

                return Response.ok(res, MediaType.APPLICATION_JSON).build();
            } catch (OpenAgencyException | JSONBException e) {
                LOGGER.error(e.getMessage());
                return Response.serverError().build();
            }
        } finally {
            LOGGER.exit(res);
        }
    }
}
