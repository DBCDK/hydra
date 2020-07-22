package dk.dbc.hydra.queue;

import java.util.List;

public class EnqueueAgencyResponse {

    private boolean validated;
    private String message;
    private List<AgencyAnalysis> agencyAnalysisList;

    public List<AgencyAnalysis> getAgencyAnalysisList() {
        return agencyAnalysisList;
    }

    public void setAgencyAnalysisList(List<AgencyAnalysis> agencyAnalysisList) {
        this.agencyAnalysisList = agencyAnalysisList;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
