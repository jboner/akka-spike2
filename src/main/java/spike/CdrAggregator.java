package spike;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class CdrAggregator extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(CdrAggregator.class);

    private final Map<String, Long> currentState = new HashMap<String, Long>();
    private final ActorRef proxyCallMonitor;
    private String etag;
    private final Publisher publisher = new Publisher(logger);
    private boolean subscriptionsInitialized;

    public CdrAggregator(ActorRef proxyCallMonitor) {
        this.proxyCallMonitor = proxyCallMonitor;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message instanceof HAState) {
                promoteMeToPrimaryNode();
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

    private void promoteMeToPrimaryNode() {
        publisher.setPrimaryNode(true);
    }

    private void handleDialogEvent(DialogEvent event) {
        String callId = event.getCallId();
        Long value = currentState.get(callId);
        if (value == null) {
            value = 0L;
        }
        value += event.getInc();
        currentState.put(callId, value);

        logger.info(event.toString());

        if (event.isDone()) {
            publishCdrEvent(callId, event.getEventId());
            currentState.remove(callId);
        }
    }

    private void publishCdrEvent(String callId, String eventId) {
        Long sum = currentState.get(callId);
        if (sum == null) {
            throw new IllegalStateException("No such dialog: " + callId);
        }
        CdrEvent cdrEvent = new CdrEvent(callId, eventId, sum);

        publisher.publish(cdrEvent);

    }

    private void initSubscriptions() {
        if (subscriptionsInitialized) {
            return;
        }
        proxyCallMonitor.sendOneWay(new Subscribe(Subscribe.Type.NORMAL, etag), getContext());
        subscriptionsInitialized = true;
    }

    @Override
    public void postStop() {
        logger.info("Stopped");
    }

    // TODO can't be done from postStop
    @SuppressWarnings("unused")
    private void removeSubscriptions() {
        subscriptionsInitialized = false;
        proxyCallMonitor.sendOneWay(new Unsubscribe(Subscribe.Type.PRIMARY_ONLY), getContext());
    }

}
