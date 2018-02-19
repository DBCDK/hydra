/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.hydra.stats;

import java.util.Date;

public class RecordSummary {

    private int agencyId;
    private int originalCount;
    private int enrichmentCount;
    private int deletedCount;
    private Date ajourDate;

    public RecordSummary(int agencyId, int originalCount, int enrichmentCount, int deletedCount, Date ajourDate) {
        this.agencyId = agencyId;
        this.originalCount = originalCount;
        this.enrichmentCount = enrichmentCount;
        this.deletedCount = deletedCount;
        this.ajourDate = ajourDate;
    }

    public int getAgencyId() {
        return agencyId;
    }

    public int getOriginalCount() {
        return originalCount;
    }

    public int getEnrichmentCount() {
        return enrichmentCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public Date getAjourDate() {
        return ajourDate;
    }

    @Override
    public String toString() {
        return "RecordSummary{" +
                "agencyId=" + agencyId +
                ", originalCount=" + originalCount +
                ", enrichmentCount=" + enrichmentCount +
                ", deletedCount=" + deletedCount +
                ", ajourDate=" + ajourDate +
                '}';
    }
}
