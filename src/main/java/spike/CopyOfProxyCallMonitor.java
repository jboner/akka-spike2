package spike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorRegistry;
import akka.actor.UntypedActor;

import com.eaio.uuid.UUID;

public class CopyOfProxyCallMonitor extends UntypedActor {

    private static Logger logger = LoggerFactory.getLogger(CopyOfProxyCallMonitor.class);

    private final Map<String, List<DialogEvent>> dialogEvents = new HashMap<String, List<DialogEvent>>();
    private ActorRef cdrAggregator;
    private final Map<Subscribe.Type, List<ActorRef>> subscribers = new HashMap<Subscribe.Type, List<ActorRef>>();
    boolean isPrimaryNode;

    public CopyOfProxyCallMonitor() {
        for (Subscribe.Type each : Subscribe.Type.values()) {
            subscribers.put(each, new ArrayList<ActorRef>());
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message instanceof SipReq) {
                handleSipReq(message);
                // TODO safe or unsafe?
                getContext().replyUnsafe("100 Trying");
            } else if (message instanceof DialogEvent) {
                handleDialogEvent(message);
            } else if (message instanceof Subscribe) {
                handleSubscribe(message);
            } else if (message instanceof Unsubscribe) {
                handleSubscribe(message);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleSubscribe(Object message) {
        Subscribe event = (Subscribe) message;
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            subscribers.get(event.getType()).add(senderRef);
        }
    }

    private void handleUnsubscribe(Object message) {
        Unsubscribe event = (Unsubscribe) message;
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            subscribers.get(event.getType()).remove(senderRef);
        }
    }

    private void handleSipReq(Object message) {
        SipReq req = (SipReq) message;
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

        publishDialogEvent(event);
        if (req.isDone()) {
            dialogEvents.remove(callId);
        }
    }

    private void promoteMeToPrimaryNode() {
        if (!isPrimaryNode) {
            initCdrAggregator();
            cdrAggregator.sendOneWay(new HAState(true));
        }
        isPrimaryNode = true;
    }

    private void handleDialogEvent(Object message) {
        DialogEvent event = (DialogEvent) message;
        String callId = event.getCallId();
        List<DialogEvent> events = dialogEvents.get(callId);
        if (events == null) {
            events = new ArrayList<DialogEvent>();
            dialogEvents.put(callId, events);
        }

        events.add(event);

        logger.info("Handle DialogEvent: " + event.toString());

        publishDialogEvent(event);
        if (event.isDone()) {
            dialogEvents.remove(callId);
        }
    }

    private DialogEvent createDialogEvent(SipReq req, int factor) {
        return new DialogEvent(req.getCallId(), req.getEventId(), factor * req.getInc(), req.isDone());
    }

    private void publishDialogEvent(DialogEvent event) {
        initBuddy();
        if (isPrimaryNode) {
            publish(event, subscribers.get(Subscribe.Type.BUDDY));
            publish(event, subscribers.get(Subscribe.Type.PRIMARY_ONLY));
        }

        publish(event, subscribers.get(Subscribe.Type.NORMAL));
    }

    private void publish(DialogEvent event, List<ActorRef> toSubscribers) {
        for (ActorRef subscriber : toSubscribers) {
            try {
                subscriber.sendOneWay(event);
                logger.info("Published from ProxyCallMonitor:\t" + event.toString());
            } catch (RuntimeException e) {
                logger.info("Failed to publish from ProxyCallMonitor:\t" + event.toString());
            }
        }
    }

    private void initBuddy() {
        if (buddy == null) {
            // TODO how to do this? actor id and registered id?
            // ActorRef[] matching =
            // ActorRegistry.actorsFor(SystemConfiguration.cdrAggregatorId);
            ActorRef[] matching = ActorRegistry.actorsFor(CopyOfProxyCallMonitor.class.getName());
            if (matching != null && matching.length > 0) {
                UUID myUuid = getContext().getUuid();
                for (ActorRef each : matching) {
                    if (!each.getUuid().equals(myUuid)) {
                        buddy = each;
                        break;
                    }
                }
            }
        }
        if (buddy == null) {
            logger.info("No Buddy");
        }
    }

    @Override
    public void postStart() {
    }

    @Override
    public void preStop() {

    }

    @Override
    public void postStop() {
        logger.info("Stopped ProxyCallMonitor");
    }
}
