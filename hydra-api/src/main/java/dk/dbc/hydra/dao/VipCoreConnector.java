package dk.dbc.hydra.dao;

import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import dk.dbc.vipcore.marshallers.LibraryRule;
import dk.dbc.vipcore.marshallers.LibraryRulesRequest;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

@Stateless
public class VipCoreConnector {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(VipCoreConnector.class);

    @Inject
    VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;

    public Set<String> getLibrariesByCatalogingTemplateSet(String template) throws VipCoreException {
        LOGGER.entry(template);
        StopWatch watch = new Log4JStopWatch("service.vipcore.getLibrariesByCatalogingTemplateSet");

        try {
            final LibraryRule libraryRule = new LibraryRule();
            libraryRule.setName(VipCoreLibraryRulesConnector.Rule.CATALOGING_TEMPLATE_SET.getValue());
            libraryRule.setString(template);
            final LibraryRulesRequest libraryRulesRequest = new LibraryRulesRequest();
            libraryRulesRequest.setLibraryRule(Collections.singletonList(libraryRule));

            return vipCoreLibraryRulesConnector.getLibraries(libraryRulesRequest);
        } finally {
            watch.stop();
            LOGGER.exit();
        }
    }
}
