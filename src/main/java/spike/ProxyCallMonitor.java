package spike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spike.SystemConfiguration.RemoteLookupInfo;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.remote.RemoteClient;

public class ProxyCallMonitor extends UntypedActor {

    private final Logger logger;
    private final Map<String, List<DialogEvent>> dialogEvents = new HashMap<String, List<DialogEvent>>();
    private final List<DialogEvent> history = new ArrayList<DialogEvent>();
    private final Publisher publisher;
    private boolean isPrimaryNode;
    private long etag;
    private boolean subscriptionsInitialized;

    public ProxyCallMonitor(SystemConfiguration.RemoteLookupInfo lookupInfo) {
        logger = LoggerFactory.getLogger(lookupInfo.id);
        getContext().setId(lookupInfo.id);
        publisher = new Publisher(logger);
        getContext().setHomeAddress(lookupInfo.host, lookupInfo.port);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message instanceof SipReq) {
                handleSipReq((SipReq) message);
                // TODO safe or unsafe?
                getContext().replyUnsafe("100 Trying");
            } else if (message instanceof DialogEvent) {
                handleDialogEvent((DialogEvent) message);
            } else if (message instanceof DialogSnapshot) {
                handleDialogSnapshot((DialogSnapshot) message);
            } else if (message instanceof Subscribe) {
                handleSubscribe((Subscribe) message);
            } else if (message instanceof Unsubscribe) {
                handleUnsubscribe((Unsubscribe) message);
            } else if (message instanceof Heartbeat) {
                handleHeartbeat((Heartbeat) message);
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

    private void handleSubscribe(Subscribe event) {
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            publisher.addSubscriber(senderRef, event);

            DialogSnapshot snapshot = createDialogSnapshot(event.getFromEtag());
            publisher.publishSnapshot(snapshot, senderRef, event.getType());
        }
    }

    private void handleUnsubscribe(Unsubscribe event) {
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            publisher.removeSubscriber(senderRef, event);
        }
    }

    private void handleSipReq(SipReq req) {
        logger.info("Handle: " + req.toString());

        promoteMeToPrimaryNode();
        String callId = req.getCallId();
        int factor = factor(callId);
        DialogEvent event = createDialogEvent(req, factor, etag + 1);
        handleDialogEvent(event);
    }

    private int factor(String callId) {
        List<DialogEvent> events = dialogEvents.get(callId);
        int factor = (events == null ? 1 : events.size() + 1);
        return factor;
    }

    private void handleDialogSnapshot(DialogSnapshot snapshot) {
        logger.info("Handle: {}", snapshot);
        subscriptionsInitialized = true;
        if (snapshot.getEtag() <= etag) {
            logger.info("Ignorig snapshot with etag {} <= current etag {} : {}", new Object[] { snapshot.getEtag(),
                    etag, snapshot });
            return;
        }
        for (DialogEvent each : snapshot.getEvents()) {
            logger.info("Replay snapshot event {}", each);
            handleDialogEvent(each);
        }
    }

    private void handleDialogEvent(DialogEvent event) {
        if (event.getEtag() <= etag) {
            logger.info("Ignorig event with etag {} <= current etag {} : {}", new Object[] { event.getEtag(), etag,
                    event });
            return;
        }
        String callId = event.getCallId();
        List<DialogEvent> events = dialogEvents.get(callId);
        if (events == null) {
            events = new ArrayList<DialogEvent>();
            dialogEvents.put(callId, events);
        }

        events.add(event);
        etag = event.getEtag();

        logger.info("Handle: {}", event);

        publisher.publish(event);
        // TODO we should clean history sometime
        history.add(event);
        if (event.isDone()) {
            dialogEvents.remove(callId);
        }
    }

    private void promoteMeToPrimaryNode() {
        if (!isPrimaryNode) {
            isPrimaryNode = true;
            publisher.setPrimaryNode(isPrimaryNode);
            HAState haState = new HAState(isPrimaryNode);
            publisher.publish(haState);
        }
    }

    private DialogEvent createDialogEvent(SipReq req, int factor, long etag) {
        return new DialogEvent(req.getCallId(), req.getEventId(), factor * req.getInc(), req.isDone(), etag);
    }

    private DialogSnapshot createDialogSnapshot(long fromEtag) {
        List<DialogEvent> events = new ArrayList<DialogEvent>();
        for (DialogEvent each : history) {
            if (each.getEtag() > fromEtag) {
                events.add(each);
            }
        }
        DialogSnapshot snapshot = new DialogSnapshot(etag, events);
        logger.info("Created snapshot: {}", snapshot);
        return snapshot;
    }

    private List<ActorRef> buddies() {
        List<ActorRef> result = new ArrayList<ActorRef>();
        // In Akka cloud there is a cluster aware ActorRegistry, to avoid
        // knowing host/port
        String myId = getContext().getId();
        for (RemoteLookupInfo each : SystemConfiguration.proxyCallMonitorInfos) {
            ActorRef buddy = RemoteClient.actorFor(each.id, each.host, each.port);
            if (!buddy.getId().equals(myId)) {
                result.add(buddy);
            }
        }
        return result;
    }

    private void initSubscriptions() {
        if (isPrimaryNode) {
            return;
        }
        if (subscriptionsInitialized) {
            return;
        }

        for (ActorRef each : buddies()) {
            subscribe(each);
        }

    }

    private void subscribe(ActorRef buddy) {
        Subscribe subscribeEvent = new Subscribe(Subscribe.Type.BUDDY, etag);
        logger.info("Send subscription: {}", subscribeEvent);
        buddy.sendOneWay(subscribeEvent, getContext());
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
        for (ActorRef each : buddies()) {
            unsubscribe(each);
        }
    }

    private void unsubscribe(ActorRef publisher) {
        publisher.sendOneWay(new Unsubscribe(Subscribe.Type.PRIMARY_ONLY), getContext());
    }
}
