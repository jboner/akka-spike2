package spike;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

public class HeartbeatTimer extends Publisher {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long interval;
    private final TimeUnit unit;
    private ScheduledFuture<?> scheduledFuture;

    public HeartbeatTimer(long interval, TimeUnit unit) {
        super(LoggerFactory.getLogger(HeartbeatTimer.class));
        this.interval = interval;
        this.unit = unit;
    }

    public void start() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Heartbeat heartbeat = new Heartbeat();
                publish(heartbeat);
            }
        };
        scheduledFuture = executor.scheduleWithFixedDelay(task, interval, interval, unit);
    }

    public void stop() {
        scheduledFuture.cancel(true);
        executor.shutdown();
    }

}
