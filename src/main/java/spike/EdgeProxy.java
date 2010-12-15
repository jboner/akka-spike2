package spike;

import static spike.SystemConfiguration.proxyCallMonitorId;
import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodePort1;
import akka.actor.ActorRef;
import akka.remote.RemoteClient;

public class EdgeProxy {

    private ActorRef servicenode;

    public static void main(String[] args) {
        EdgeProxy manager = new EdgeProxy();
        manager.simulateLoad();

    }

    public EdgeProxy() {
        init();
    }

    private void init() {
        // TODO use registry instead?
        servicenode = RemoteClient.actorFor(proxyCallMonitorId, servicenodeHost1, servicenodePort1);
    }

    public void simulateLoad() {
        for (int i = 0; i < 1000; i++) {
            produce(i);
        }
    }

    private void produce(int i) {
        boolean done = i % 4 == 0;
        int inc = i % 10;
        String callId = "d" + (i % 3);
        SipReq req = new SipReq(callId, inc, done);
        // TODO how to detect success/fail, know if service is available?
        servicenode.sendOneWay(req);
    }

}
