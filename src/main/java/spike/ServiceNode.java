package spike;

import static akka.actor.UntypedActor.actorOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActorFactory;
import akka.remote.RemoteServer;

public class ServiceNode {

    private final SystemConfiguration.RemoteLookupInfo proxyCallMonitorInfo;
    private final SystemConfiguration.RemoteLookupInfo cdrAggregatorInfo;
    private RemoteServer servicenodeServer;
    private ActorRef proxyCallMonitor;
    private ActorRef cdrAggregator;
    private final HeartbeatTimer heartbeatTimer = new HeartbeatTimer(10, SECONDS);

    public static void main(String[] args) {
        int i = 0;
        if (args.length > 0) {
            i = Integer.valueOf(args[0]);
        }
        ServiceNode servicenode = new ServiceNode(i);
        servicenode.start();
    }

    public ServiceNode() {
        this(0);
    }

    public ServiceNode(int nodeIndex) {
        this.proxyCallMonitorInfo = SystemConfiguration.proxyCallMonitorInfos[nodeIndex];
        this.cdrAggregatorInfo = SystemConfiguration.cdrAggregatorInfos[nodeIndex];
    }

    public void start() {
        startRemoteServer();
        startActors();
    }

    private void startRemoteServer() {
        servicenodeServer = new RemoteServer();
        servicenodeServer.start(proxyCallMonitorInfo.host, proxyCallMonitorInfo.port);
    }

    private void startActors() {
        proxyCallMonitor = actorOf(new UntypedActorFactory() {
            @Override
            public Actor create() {
                return new ProxyCallMonitor(proxyCallMonitorInfo.id);
            }
        });
        cdrAggregator = actorOf(new UntypedActorFactory() {
            @Override
            public Actor create() {
                return new CdrAggregator(cdrAggregatorInfo.id, proxyCallMonitor);
            }
        });
        servicenodeServer.register(proxyCallMonitorInfo.id, proxyCallMonitor);
        servicenodeServer.register(cdrAggregatorInfo.id, cdrAggregator);
        // TODO will it be local invokations between proxyCallMonitor and
        // cdrAggregator?

        startHeartbeatTimer();
    }

    private void startHeartbeatTimer() {
        heartbeatTimer.addSubscriber(proxyCallMonitor, new Subscribe(Subscribe.Type.NORMAL, null));
        heartbeatTimer.addSubscriber(cdrAggregator, new Subscribe(Subscribe.Type.NORMAL, null));
        heartbeatTimer.start();
    }

    public void stop() {
        heartbeatTimer.stop();
        cdrAggregator.stop();
        proxyCallMonitor.stop();
        servicenodeServer.shutdown();
    }
}
