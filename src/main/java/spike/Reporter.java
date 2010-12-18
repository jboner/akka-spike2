package spike;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spike.SystemConfiguration.RemoteLookupInfo;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.remote.RemoteClient;
import akka.remote.RemoteClientConnected;
import akka.remote.RemoteClientDisconnected;
import akka.remote.RemoteClientError;
import akka.remote.RemoteClientShutdown;
import akka.remote.RemoteClientStarted;

class Reporter extends UntypedActor {

    private final Logger logger;
    private long etag;
    private boolean subscriptionsInitialized;

    public Reporter(String id) {
        logger = LoggerFactory.getLogger(id);
        getContext().setId(id);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message instanceof CdrEvent) {
                handleCdrEvent((CdrEvent) message);
            } else if (message instanceof CdrSnapshot) {
                handleCdrSnapshot((CdrSnapshot) message);
            } else if (message instanceof Heartbeat) {
                handleHeartbeat((Heartbeat) message);
            } else if (message instanceof RemoteClientError) {
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
            } else {
                logger.info("Unknown {}", message);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleHeartbeat(Heartbeat message) {
        initSubscriptions();
    }

    private void handleCdrSnapshot(CdrSnapshot snapshot) {
        logger.info("Handle: {}", snapshot);
        if (snapshot.getEtag() <= etag) {
            return;
        }
        for (CdrEvent each : snapshot.getEvents()) {
            handleCdrEvent(each);
        }
    }

    private void handleCdrEvent(CdrEvent cdrEvent) {
        if (cdrEvent.getEtag() <= etag) {
            logger.info("Ignorig event with etag {} <= current etag {} : {}", new Object[] { cdrEvent.getEtag(), etag,
                    cdrEvent });
            return;
        }
        logger.info("Handle: {}", cdrEvent.toString());

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./logs/cdr"
                    + ".txt", true), "UTF-8")));
            writer.println(cdrEvent.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        etag = cdrEvent.getEtag();
    }

    private List<ActorRef> interestedInPublishers() {
        List<ActorRef> result = new ArrayList<ActorRef>();
        for (RemoteLookupInfo each : SystemConfiguration.cdrAggregatorInfos) {
            ActorRef publisher = RemoteClient.actorFor(each.id, each.host, each.port);
            RemoteClient.clientFor(each.id, each.port).addListener(getContext());
            result.add(publisher);
        }
        return result;
    }

    private void addRemoteClientListeners() {
        for (RemoteLookupInfo each : SystemConfiguration.cdrAggregatorInfos) {
            RemoteClient.clientFor(each.id, each.port).addListener(getContext());
        }
    }

    private void initSubscriptions() {
        if (subscriptionsInitialized) {
            return;
        }
        for (ActorRef each : interestedInPublishers()) {
            subscribe(each);
        }
        subscriptionsInitialized = true;
    }

    private void subscribe(ActorRef publisher) {
        publisher.sendOneWay(new Subscribe(Subscribe.Type.PRIMARY_ONLY, etag), getContext());
    }

    @Override
    public void preStart() {
        logger.info("Started");
    }

    @Override
    public void postStop() {
        logger.info("Stopped");
    }

    // TODO can't be done from postStop
    @SuppressWarnings("unused")
    private void removeSubscriptions() {
        subscriptionsInitialized = false;
        for (ActorRef each : interestedInPublishers()) {
            unsubscribe(each);
        }
    }

    private void unsubscribe(ActorRef publisher) {
        publisher.sendOneWay(new Unsubscribe(Subscribe.Type.PRIMARY_ONLY), getContext());
    }
}
