/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.dao;

import dk.dbc.openagency.client.OpenAgencyException;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.hydra.common.ApplicationConstants;
import dk.dbc.hydra.common.EnvironmentVariables;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Set;

@Stateless
public class OpenAgencyConnector {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(OpenAgencyConnector.class);

    private OpenAgencyServiceFromURL service;

    @EJB
    EnvironmentVariables environmentVariables;

    @PostConstruct
    public void postConstruct() {
        LOGGER.entry();
        StopWatch watch = new Log4JStopWatch("service.openagency.init");

        if (!System.getenv().containsKey(ApplicationConstants.OPENAGENCY_URL)) {
            throw new RuntimeException("OPENAGENCY_URL must have a value");
        }

        try {
            OpenAgencyServiceFromURL.Builder builder = OpenAgencyServiceFromURL.builder();
            builder = builder.
                    connectTimeout(Integer.parseInt(environmentVariables.getenv().getOrDefault(
                            ApplicationConstants.OPENAGENCY_CONNECT_TIMEOUT,
                            ApplicationConstants.OPENAGENCY_CONNECT_TIMEOUT_DEFAULT))).
                    requestTimeout(Integer.parseInt(environmentVariables.getenv().getOrDefault(
                            ApplicationConstants.OPENAGENCY_REQUEST_TIMEOUT,
                            ApplicationConstants.OPENAGENCY_REQUEST_TIMEOUT_DEFAULT))).
                    setCacheAge(Integer.parseInt(environmentVariables.getenv().getOrDefault(
                            ApplicationConstants.OPENAGENCY_CACHE_AGE,
                            ApplicationConstants.OPENAGENCY_CACHE_AGE_DEFAULT)));

            service = builder.build(environmentVariables.getenv().get(ApplicationConstants.OPENAGENCY_URL));
        } finally {
            watch.stop();
            LOGGER.exit();
        }

    }

    public Set<String> getLibrariesByCatalogingTemplateSet(String template) throws OpenAgencyException {
        LOGGER.entry(template);
        StopWatch watch = new Log4JStopWatch("service.openagency.getLibrariesByCatalogingTemplateSet");

        try {
            return service.libraryRules().getLibrariesByCatalogingTemplateSet(template);
        } finally {
            watch.stop();
            LOGGER.exit();
        }
    }

}