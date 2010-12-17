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

import com.eaio.uuid.UUID;

public class ProxyCallMonitor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCallMonitor.class);

    private final Map<String, List<DialogEvent>> dialogEvents = new HashMap<String, List<DialogEvent>>();
    private final Publisher publisher = new Publisher(logger);
    private boolean isPrimaryNode;
    private String etag;
    private boolean subscriptionsInitialized;

    public ProxyCallMonitor() {
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
            } else if (message instanceof Subscribe) {
                handleSubscribe((Subscribe) message);
            } else if (message instanceof Unsubscribe) {
                handleUnsubscribe((Unsubscribe) message);
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

    private void handleSubscribe(Subscribe event) {
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            publisher.addSubscriber(senderRef, event);
        }
    }

    private void handleUnsubscribe(Unsubscribe event) {
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            publisher.removeSubscriber(senderRef, event);
        }
    }

    private void handleSipReq(SipReq req) {
        promoteMeToPrimaryNode();
        String callId = req.getCallId();
        List<DialogEvent> events = dialogEvents.get(callId);
        if (events == null) {
            events = new ArrayList<DialogEvent>();
            dialogEvents.put(callId, events);
        }

        int factor = events.size() + 1;
        DialogEvent event = createDialogEvent(req, factor);

        events.add(event);

        logger.info("Handle SipReq: " + req.toString());

        publisher.publish(event);
        if (req.isDone()) {
            dialogEvents.remove(callId);
        }
    }

    private void promoteMeToPrimaryNode() {
        if (!isPrimaryNode) {
            publisher.setPrimaryNode(true);
        }
        isPrimaryNode = true;
    }

    private void handleDialogEvent(DialogEvent event) {
        String callId = event.getCallId();
        List<DialogEvent> events = dialogEvents.get(callId);
        if (events == null) {
            events = new ArrayList<DialogEvent>();
            dialogEvents.put(callId, events);
        }

        events.add(event);

        logger.info("Handle DialogEvent: " + event.toString());

        publisher.publish(event);
        if (event.isDone()) {
            dialogEvents.remove(callId);
        }
    }

    private DialogEvent createDialogEvent(SipReq req, int factor) {
        return new DialogEvent(req.getCallId(), req.getEventId(), factor * req.getInc(), req.isDone());
    }

    private List<ActorRef> buddies() {
        List<ActorRef> result = new ArrayList<ActorRef>();
        // In Akka cloud there is a cluster aware ActorRegistry, to avoid
        // knowing host/port
        UUID myUuid = getContext().getUuid();
        for (RemoteLookupInfo each : SystemConfiguration.proxyCallMonitorInfos) {
            ActorRef buddy = RemoteClient.actorFor(each.id, each.host, each.port);
            if (!buddy.getUuid().equals(myUuid)) {
                result.add(buddy);
            }
        }
        return result;
    }

    private void initSubscriptions() {
        // TODO PROBLEM with lookup, uuid of RemoteActorRef is not same as
        // myUuid
        if (true) {
            return;
        }
        if (subscriptionsInitialized) {
            return;
        }
        for (ActorRef each : buddies()) {
            subscribe(each);
        }
        subscriptionsInitialized = true;
    }

    private void subscribe(ActorRef buddy) {
        buddy.sendOneWay(new Subscribe(Subscribe.Type.BUDDY, etag), getContext());
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
