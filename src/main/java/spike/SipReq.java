package spike;

import java.io.Serializable;

public class SipReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String callId;
    private final int inc;
    private final boolean done;

    public SipReq(String callId, int inc, boolean done) {
        this.callId = callId;
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

    @Override
    public String toString() {
        return "SipReq: " + callId + "\t => " + inc + (done ? " DONE" : "");
    }

}
