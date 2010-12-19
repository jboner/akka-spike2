package spike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class CdrAggregator extends UntypedActor {

    private final Logger logger;
    private final Map<String, Long> currentState = new HashMap<String, Long>();
    private final ActorRef proxyCallMonitor;
    private long inEtag;
    private long outEtag;
    private final List<CdrEvent> history = new ArrayList<CdrEvent>();
    private final Publisher publisher;
    private boolean subscriptionsInitialized;

    public CdrAggregator(SystemConfiguration.RemoteLookupInfo lookupInfo, ActorRef proxyCallMonitor) {
        logger = LoggerFactory.getLogger(lookupInfo.id);
        getContext().setId(lookupInfo.id);
        this.proxyCallMonitor = proxyCallMonitor;
        publisher = new Publisher(logger);
        getContext().setHomeAddress(lookupInfo.host, lookupInfo.port);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message instanceof DialogEvent) {
                handleDialogEvent((DialogEvent) message);
            } else if (message instanceof DialogSnapshot) {
                handleDialogSnapshot((DialogSnapshot) message);
            } else if (message instanceof Subscribe) {
                handleSubscribe((Subscribe) message);
            } else if (message instanceof Unsubscribe) {
                handleUnsubscribe((Unsubscribe) message);
            } else if (message instanceof Heartbeat) {
                handleHeartbeat((Heartbeat) message);
            } else if (message instanceof HAState) {
                handleHAState((HAState) message);
            } else {
                logger.info("Unknown {}", message);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleDialogSnapshot(DialogSnapshot snapshot) {
        logger.info("Handle: {}", snapshot);
        if (snapshot.getEtag() <= inEtag) {
            return;
        }
        for (DialogEvent each : snapshot.getEvents()) {
            handleDialogEvent(each);
        }
    }

    private void handleHeartbeat(Heartbeat message) {
        initSubscriptions();
    }

    private void handleSubscribe(Subscribe event) {
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            publisher.addSubscriber(senderRef, event);

            CdrSnapshot snapshot = createSnapshot(event.getFromEtag());
            publisher.publishSnapshot(snapshot, senderRef, event.getType());
        }
    }

    private void handleUnsubscribe(Unsubscribe event) {
        if (getContext().getSender().isDefined()) {
            ActorRef senderRef = getContext().getSender().get();
            publisher.removeSubscriber(senderRef, event);
        }
    }

    private void handleHAState(HAState event) {
        logger.info("Handle: {}", event);
        publisher.setPrimaryNode(event.isPrimaryNode());
    }

    private void handleDialogEvent(DialogEvent event) {
        if (event.getEtag() <= inEtag) {
            logger.info("Ignorig event with etag {} <= current etag {} : {}", new Object[] { event.getEtag(), inEtag,
                    event });
            return;
        }
        String callId = event.getCallId();
        Long value = currentState.get(callId);
        if (value == null) {
            value = 0L;
        }
        value += event.getInc();
        currentState.put(callId, value);
        inEtag = event.getEtag();

        logger.info("Handle: {}", event);

        if (event.isDone()) {
            if (publisher.isPrimaryNode() && getContext().getId().endsWith("-2")) {
                // TODO these never reach Reporter after failover
                logger.info("Should publish from secondary: {}", event);
            }
            publishCdrEvent(callId, event.getEventId(), event.getEtag());
            currentState.remove(callId);
        }
    }

    private void publishCdrEvent(String callId, String eventId, long etag) {
        outEtag = etag;
        CdrEvent cdrEvent = createCdrEvent(callId, eventId, outEtag);
        history.add(cdrEvent);
        // TODO we should clean history sometime
        publisher.publish(cdrEvent);
    }

    private CdrEvent createCdrEvent(String callId, String eventId, long etag) {
        Long sum = currentState.get(callId);
        if (sum == null) {
            throw new IllegalStateException("No such dialog: " + callId);
        }
        CdrEvent cdrEvent = new CdrEvent(callId, eventId, sum, etag);
        return cdrEvent;
    }

    private CdrSnapshot createSnapshot(long fromEtag) {
        List<CdrEvent> events = new ArrayList<CdrEvent>();
        for (CdrEvent each : history) {
            if (each.getEtag() > fromEtag) {
                events.add(each);
            }
        }
        return new CdrSnapshot(outEtag, events);
    }

    private void initSubscriptions() {
        if (subscriptionsInitialized) {
            return;
        }
        proxyCallMonitor.sendOneWay(new Subscribe(Subscribe.Type.NORMAL, inEtag), getContext());
        subscriptionsInitialized = true;
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
        proxyCallMonitor.sendOneWay(new Unsubscribe(Subscribe.Type.PRIMARY_ONLY), getContext());
    }

}
