package dk.dbc.hydra.queue;

public class EnqueueAgencyRequest {

    private String agencies;
    private String worker;
    private boolean enqueueDBCAsEnrichment;
    private Integer priority;

    public String getAgencies() {
        return agencies;
    }

    public void setAgencies(String agencies) {
        this.agencies = agencies;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public boolean isEnqueueDBCAsEnrichment() {
        return enqueueDBCAsEnrichment;
    }

    public void setEnqueueDBCAsEnrichment(boolean enqueueDBCAsEnrichment) {
        this.enqueueDBCAsEnrichment = enqueueDBCAsEnrichment;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
