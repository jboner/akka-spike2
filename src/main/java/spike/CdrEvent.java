package spike;

import java.io.Serializable;

public class CdrEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String callId;
    private final long sum;

    public CdrEvent(String callId, long sum) {
        this.callId = callId;
        this.sum = sum;
    }

    public String getCallId() {
        return callId;
    }

    public long getSum() {
        return sum;
    }

    @Override
    public String toString() {
        return "CdrEvent: " + callId + "\t => " + sum;
    }

}
