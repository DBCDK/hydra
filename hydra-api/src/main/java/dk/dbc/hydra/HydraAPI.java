/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra;

import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Interceptors(StopwatchInterceptor.class)
@Stateless
@Path(ApplicationConstants.API_HYDRA)
public class HydraAPI {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(HydraAPI.class);

    @Inject
    @ConfigProperty(name = "INSTANCE_NAME", defaultValue = "INSTANCE_NAME not set")
    String INSTANCE_NAME;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_HYDRA_STATUS)
    public Response getStatus() {
        return Response.ok(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path(ApplicationConstants.API_HYDRA_INSTANCE_NAME)
    public Response getInstanceName() {
        LOGGER.entry();
        String res = "";
        try {
            JsonObjectBuilder jsonObject = Json.createObjectBuilder();
            jsonObject.add("value", INSTANCE_NAME);

            res = jsonObject.build().toString();

            return Response.ok(res, MediaType.APPLICATION_JSON).build();
        } finally {
            LOGGER.exit(res);
        }
    }
}
