package dk.dbc.hydra.queue;

public class RecordEnqueueResult {

    private String bibliographicRecordId;
    private int agencyId;
    private String worker;
    private boolean queued;

    public RecordEnqueueResult() {
    }

    public RecordEnqueueResult(String bibliographicRecordId, int agencyId, String worker, boolean queued) {
        this.bibliographicRecordId = bibliographicRecordId;
        this.agencyId = agencyId;
        this.worker = worker;
        this.queued = queued;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public void setBibliographicRecordId(String bibliographicRecordId) {
        this.bibliographicRecordId = bibliographicRecordId;
    }

    public int getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(int agencyId) {
        this.agencyId = agencyId;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public boolean isQueued() {
        return queued;
    }

    public void setQueued(boolean queued) {
        this.queued = queued;
    }
}
