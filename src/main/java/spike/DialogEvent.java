package spike;

import java.io.Serializable;

public class DialogEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String callId;
    private final int inc;
    private final boolean done;
    private final String eventId;
    private final long etag;

    public DialogEvent(String callId, String eventId, int inc, boolean done, long etag) {
        this.callId = callId;
        this.eventId = eventId;
        this.inc = inc;
        this.done = done;
        this.etag = etag;
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

    public long getEtag() {
        return etag;
    }

    @Override
    public String toString() {
        return "DialogEvent: " + eventId + " - " + callId + "\t => " + inc + (done ? " DONE" : "");
    }

}