package spike;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Heartbeat implements Serializable {
    private static final long serialVersionUID = 1517001694611300104L;
    private final long time;

    public Heartbeat() {
        this.time = System.currentTimeMillis();
    }

    public Heartbeat(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "Heartbeat: " + formatTime();
    }

    private String formatTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(time));
    }

}
