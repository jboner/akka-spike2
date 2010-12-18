package spike;

import java.io.Serializable;

public class CdrEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String callId;
    private final long sum;
    private final String eventId;
    private final long etag;

    public CdrEvent(String callId, String eventId, long sum, long etag) {
        this.callId = callId;
        this.eventId = eventId;
        this.sum = sum;
        this.etag = etag;
    }

    public String getCallId() {
        return callId;
    }

    public long getSum() {
        return sum;
    }

    public String getEventId() {
        return eventId;
    }

    public long getEtag() {
        return etag;
    }

    @Override
    public String toString() {
        return "CdrEvent: " + eventId + " - " + callId + "\t => " + sum;
    }

}
