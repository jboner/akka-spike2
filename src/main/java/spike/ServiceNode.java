package spike;

import static akka.actor.UntypedActor.actorOf;
import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;
import akka.actor.ActorRef;
import akka.remote.RemoteServer;

public class ServiceNode {

    private RemoteServer servicenodeServer;
    private ActorRef proxyCallMonitorActor;
    private ActorRef cdrAggregator;

    public static void main(String[] args) {
        ServiceNode servicenode = new ServiceNode();
        if (args[0].equals("1")) {
            servicenode.start(servicenodeHost1, servicenodePort1);
        } else {
            servicenode.start(servicenodeHost2, servicenodePort2);
        }
    }

    public void start() {
        start(servicenodeHost1, servicenodePort1);
    }

    public void start(String host, int port) {
        servicenodeServer = new RemoteServer();
        servicenodeServer.start(host, port);
        startActors();
    }

    private void startActors() {
        proxyCallMonitorActor = actorOf(ProxyCallMonitor.class);
        servicenodeServer.register(SystemConfiguration.proxyCallMonitorId, proxyCallMonitorActor);
        cdrAggregator = actorOf(CdrAggregator.class).start();
    }

    public void stop() {
        cdrAggregator.stop();
        proxyCallMonitorActor.stop();
        servicenodeServer.shutdown();
    }
}
