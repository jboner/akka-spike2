package spike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private final Publisher publisher;
    private boolean isPrimaryNode;
    private long etag;
    private boolean subscriptionsInitialized;

    public ProxyCallMonitor(String id) {
        logger = LoggerFactory.getLogger(id);
        getContext().setId(id);
        publisher = new Publisher(logger);
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

            DialogSnapshot snapshot;
            if (event.getFromEtag() < etag) {
                snapshot = createDialogSnapshot(event.getFromEtag());
            } else {
                snapshot = new DialogSnapshot(etag, new ArrayList<DialogEvent>());
            }
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
        if (snapshot.getEtag() <= etag) {
            return;
        }
        for (DialogEvent each : snapshot.getEvents()) {
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
        List<DialogEvent> allEvents = new ArrayList<DialogEvent>();
        for (List<DialogEvent> eachList : dialogEvents.values()) {
            for (DialogEvent each : eachList) {
                if (each.getEtag() > fromEtag) {
                    allEvents.add(each);
                }
            }
        }
        Collections.sort(allEvents, new Comparator<DialogEvent>() {
            @Override
            public int compare(DialogEvent o1, DialogEvent o2) {
                long etag1 = o1.getEtag();
                long etag2 = o2.getEtag();
                return (etag1 < etag2 ? -1 : (etag1 == etag2 ? 0 : 1));
            }
        });

        return new DialogSnapshot(etag, allEvents);
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
        if (subscriptionsInitialized) {
            return;
        }
        for (ActorRef each : buddies()) {
            subscribe(each);
        }
        subscriptionsInitialized = true;
    }

    private void subscribe(ActorRef buddy) {
        // TODO probably we should reduce timeout for this
        buddy.sendOneWay(new Subscribe(Subscribe.Type.BUDDY, etag), getContext());
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
