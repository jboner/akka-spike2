package spike;

import java.io.Serializable;

public class CdrEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String callId;
    private final long sum;
    private final String eventId;

    public CdrEvent(String callId, String eventId, long sum) {
        this.callId = callId;
        this.eventId = eventId;
        this.sum = sum;
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

    @Override
    public String toString() {
        return "CdrEvent: " + eventId + " - " + callId + "\t => " + sum;
    }

}
