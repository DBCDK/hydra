package dk.dbc.hydra.queue;

import java.util.List;

public class EnqueueRecordsResponse {

    private boolean validated;
    private String message;
    private List<RecordEnqueueResult> recordEnqueueResultList;

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

    public List<RecordEnqueueResult> getRecordEnqueueResultList() {
        return recordEnqueueResultList;
    }

    public void setRecordEnqueueResultList(List<RecordEnqueueResult> recordEnqueueResultList) {
        this.recordEnqueueResultList = recordEnqueueResultList;
    }
}
