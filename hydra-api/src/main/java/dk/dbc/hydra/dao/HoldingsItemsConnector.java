/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.dao;

import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.hydra.timer.Stopwatch;
import dk.dbc.hydra.timer.StopwatchInterceptor;
import dk.dbc.rawrepo.RecordId;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Interceptors(StopwatchInterceptor.class)
@Stateless
public class HoldingsItemsConnector {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(HoldingsItemsConnector.class);

    @PersistenceContext(unitName = "holdingsItems_PU")
    private EntityManager entityManager;

    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<RecordId> getHoldingsRecords(Set<Integer> agencies) throws HoldingsItemsException, SQLException {
        LOGGER.entry(agencies);
        Set<RecordId> result = new HashSet<>();
        try {
            for (Integer agencyId : agencies) {
                HoldingsItemsDAO holdingsItemsDAO = HoldingsItemsDAO.newInstance(entityManager);
                Set<String> bibliographicRecordIds = holdingsItemsDAO.getBibliographicIds(agencyId);

                for (String bibliographicRecordId : bibliographicRecordIds) {
                    result.add(new RecordId(bibliographicRecordId, agencyId));
                }
            }
            return result;
        } finally {
            LOGGER.exit(result);
        }
    }
}
