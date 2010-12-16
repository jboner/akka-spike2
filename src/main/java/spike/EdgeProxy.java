package spike;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static spike.SystemConfiguration.proxyCallMonitorId;
import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.dispatch.Future;
import akka.remote.RemoteClient;

public class EdgeProxy {

    private static Logger logger = LoggerFactory.getLogger(EdgeProxy.class);
    private ActorRef servicenode1;
    private ActorRef servicenode2;
    private ActorRef activeServiceNode;

    public static void main(String[] args) {
        EdgeProxy manager = new EdgeProxy();
        int n = 1000;
        long duration = 10;
        if (args.length > 0) {
            n = Integer.valueOf(args[0]);
        }
        if (args.length > 1) {
            duration = Long.valueOf(args[1]);
        }
        manager.simulateLoad(n, duration, TimeUnit.SECONDS);

    }

    public EdgeProxy() {
        init();
    }

    private void init() {
        // TODO use registry instead?
        servicenode1 = RemoteClient.actorFor(proxyCallMonitorId, servicenodeHost1, servicenodePort1);
        servicenode2 = RemoteClient.actorFor(proxyCallMonitorId, servicenodeHost2, servicenodePort2);
        activeServiceNode = servicenode1;
    }

    public void simulateLoad(int n, long duration, TimeUnit durationUnit) {
        long sleepMillis = MILLISECONDS.convert(duration, durationUnit) / n;
        sleepMillis--;

        for (int i = 0; i < n; i++) {
            produce(i, 0, sleepMillis);
        }
    }

    private void produce(int i, int retry, long sleepMillis) {
        if (retry > 5) {
            throw new RuntimeException("Too many retry attempts");
        }

        boolean done = i % 4 == 0;
        int inc = i % 10;
        String callId = "d" + (i % 3);
        SipReq req = new SipReq(callId, String.valueOf(i), inc, done);
        logger.info("Sending from EdgeProxy: " + req);
        // TODO how to detect success/fail, know if service is available?
        // try {
        // servicenode.sendOneWay(req);
        // } catch (RuntimeException e) {
        // e.printStackTrace();
        // }

        try {
            // servicenode.setTimeout(2000);
            Future<?> future = activeServiceNode.sendRequestReplyFuture(req);
            future.await();
            if (future.isCompleted()) {
                logger.info("Reply from from EdgeProxy: " + req);
            }
            // TODO what is diff between isExpired and TimeoutException?
            // if (future.isExpired()) {
            // logger.info("Future Timeout from from EdgeProxy: " + req);
            // toggleServiceNode();
            // produce(i, retry + 1);
            // }
        } catch (RuntimeException e) {
            logger.info("Timeout from from EdgeProxy: " + req);
            toggleServiceNode();
            produce(i, retry + 1, sleepMillis);
        }

        sleep(sleepMillis);
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private void toggleServiceNode() {
        activeServiceNode = (activeServiceNode == servicenode1 ? servicenode2 : servicenode1);

    }
}
