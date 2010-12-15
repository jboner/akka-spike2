package spike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorRegistry;
import akka.actor.UntypedActor;

import com.eaio.uuid.UUID;

public class ProxyCallMonitor extends UntypedActor {

    private static Logger logger = LoggerFactory.getLogger(ProxyCallMonitor.class);

    private final Map<String, List<DialogEvent>> dialogEvents = new HashMap<String, List<DialogEvent>>();
    private ActorRef cdrAggregator;
    private ActorRef buddy;
    boolean isPrimaryNode;

    public ProxyCallMonitor() {
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
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
        logger.info("Publishing from ProxyCallMonitor:\t" + event.toString());

        initBuddy();
        if (isPrimaryNode && buddy != null) {
            try {
                buddy.sendOneWay(event);
                logger.info("Published from ProxyCallMonitor to Buddy:\t" + event.toString());
            } catch (RuntimeException e) {
                logger.info("Failed to publish from ProxyCallMonitor to Buddy:\t" + event.toString());
            }
        }

        initCdrAggregator();

        if (cdrAggregator != null) {
            cdrAggregator.sendOneWay(event);
            logger.info("Published from ProxyCallMonitor to CdrAggregator:\t" + event.toString());
        }
    }

    private void initBuddy() {
        if (buddy == null) {
            // TODO how to do this? actor id and registered id?
            // ActorRef[] matching =
            // ActorRegistry.actorsFor(SystemConfiguration.cdrAggregatorId);
            ActorRef[] matching = ActorRegistry.actorsFor(ProxyCallMonitor.class.getName());
            if (matching.length > 0) {
                UUID myUuid = getContext().getUuid();
                logger.info("My uuid: " + myUuid);
                for (ActorRef each : Arrays.asList(matching)) {
                    logger.info("Possible match: " + each.getUuid());
                    if (!each.getUuid().equals(myUuid)) {
                        buddy = each;
                        logger.info("Found match: " + each.getUuid());
                        break;
                    }
                }
            }
        }
        if (buddy == null) {
            logger.info("No Buddy");
        }
    }

    private void initCdrAggregator() {
        if (cdrAggregator == null) {
            cdrAggregator = actorOf(CdrAggregator.class).start();
        }
    }

    @Override
    public void postStop() {
        logger.info("Stopped ProxyCallMonitor");
    }
}
