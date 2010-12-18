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

class Reporter extends UntypedActor {

    private final Logger logger;
    private boolean firstTime = true;
    private String etag;
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
            } else if (message instanceof Heartbeat) {
                handleHeartbeat((Heartbeat) message);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleHeartbeat(Heartbeat message) {
        initSubscriptions();
    }

    private void handleCdrEvent(CdrEvent cdrEvent) {
        logger.info("Handle: {}", cdrEvent.toString());
        PrintWriter writer = null;
        try {
            boolean append = !firstTime;
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./logs/cdr"
                    + ".txt", append), "UTF-8")));
            writer.println(cdrEvent.toString());
            firstTime = false;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private List<ActorRef> interestedInPublishers() {
        List<ActorRef> result = new ArrayList<ActorRef>();
        for (RemoteLookupInfo each : SystemConfiguration.cdrAggregatorInfos) {
            ActorRef publisher = RemoteClient.actorFor(each.id, each.host, each.port);
            result.add(publisher);
        }
        return result;
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
