package spike;

import java.io.Serializable;

public class SipReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String callId;
    private final int inc;
    private final boolean done;
    private final String eventId;

    public SipReq(String callId, String eventId, int inc, boolean done) {
        this.callId = callId;
        this.eventId = eventId;
        this.inc = inc;
        this.done = done;
    }

    public String getCallId() {
        return callId;
    }

    public int getInc() {
        return inc;
    }

    public boolean isDone() {
        return done;
    }

    public String getEventId() {
        return eventId;
    }

    @Override
    public String toString() {
        return "SipReq: " + eventId + " - " + callId + "\t => " + inc + (done ? " DONE" : "");
    }

}
