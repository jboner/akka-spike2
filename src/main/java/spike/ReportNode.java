package spike;

import static akka.actor.UntypedActor.actorOf;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spike.SystemConfiguration.RemoteLookupInfo;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.remote.RemoteClient;
import akka.remote.RemoteClientConnected;
import akka.remote.RemoteClientDisconnected;
import akka.remote.RemoteClientError;
import akka.remote.RemoteClientShutdown;
import akka.remote.RemoteClientStarted;
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
        addRemoteClientListeners();
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
        heartbeatTimer.addSubscriber(reporter, new Subscribe(Subscribe.Type.NORMAL, 0));
        heartbeatTimer.start();
    }

    public void stop() {
        heartbeatTimer.stop();
        reporter.stop();
        reportnode.shutdown();
    }

    private void addRemoteClientListeners() {
        ActorRef listener = actorOf(RemoteClientListener.class).start();
        for (RemoteLookupInfo each : SystemConfiguration.cdrAggregatorInfos) {
            RemoteClient.clientFor(each.id, each.port).addListener(listener);
        }
    }

    public static class RemoteClientListener extends UntypedActor {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        @Override
        public void onReceive(Object message) throws Exception {
            try {
                if (message instanceof RemoteClientError) {
                    RemoteClientError event = (RemoteClientError) message;
                    Throwable cause = event.getCause();
                    RemoteClient client = event.getClient();
                    logger.error("RemoteClientError");
                } else if (message instanceof RemoteClientConnected) {
                    RemoteClientConnected event = (RemoteClientConnected) message;
                    RemoteClient client = event.getClient();
                    logger.info("RemoteClientConnected");
                } else if (message instanceof RemoteClientDisconnected) {
                    RemoteClientDisconnected event = (RemoteClientDisconnected) message;
                    RemoteClient client = event.getClient();
                    logger.info("RemoteClientDisconnected");
                } else if (message instanceof RemoteClientStarted) {
                    RemoteClientStarted event = (RemoteClientStarted) message;
                    RemoteClient client = event.getClient();
                    logger.error("RemoteClientStarted");
                } else if (message instanceof RemoteClientShutdown) {
                    RemoteClientShutdown event = (RemoteClientShutdown) message;
                    RemoteClient client = event.getClient();
                    logger.error("RemoteClientShutdown");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
