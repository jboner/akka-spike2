package spike;

import static spike.SystemConfiguration.proxyCallMonitorId;
import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;

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
        manager.simulateLoad();

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

    public void simulateLoad() {
        for (int i = 0; i < 1000; i++) {
            produce(i, 0);
        }
    }

    private void produce(int i, int retry) {
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
            produce(i, retry + 1);
        }

        sleep(10);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void toggleServiceNode() {
        activeServiceNode = (activeServiceNode == servicenode1 ? servicenode2 : servicenode1);

    }
}
