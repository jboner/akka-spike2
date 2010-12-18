package spike;

import static akka.actor.UntypedActor.actorOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActorFactory;
import akka.remote.RemoteServer;

public class ReportNode {

    private final SystemConfiguration.RemoteLookupInfo lookupInfo = SystemConfiguration.reporterInfo;
    private RemoteServer reportnode;
    private ActorRef reporter;
    private final HeartbeatTimer heartbeatTimer = new HeartbeatTimer(1, SECONDS);

    public static void main(String[] args) {
        ReportNode manager = new ReportNode();
        manager.start();
    }

    public void start() {
        startRemoteServer();
        startActors();
        startHeartbeatTimer();
    }

    private void startRemoteServer() {
        reportnode = new RemoteServer();
        reportnode.start(lookupInfo.host, lookupInfo.port);
    }

    private void startActors() {
        reporter = actorOf(new UntypedActorFactory() {
            @Override
            public Actor create() {
                return new Reporter(lookupInfo.id);
            }
        });
        reportnode.register(lookupInfo.id, reporter);
    }

    private void startHeartbeatTimer() {
        heartbeatTimer.addSubscriber(reporter, new Subscribe(Subscribe.Type.NORMAL, null));
        heartbeatTimer.start();
    }

    public void stop() {
        heartbeatTimer.stop();
        reporter.stop();
        reportnode.shutdown();
    }

}
