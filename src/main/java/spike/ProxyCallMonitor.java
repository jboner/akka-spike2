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

public class ProxyCallMonitor extends UntypedActor {

    private static Logger logger = LoggerFactory.getLogger(ProxyCallMonitor.class);

    private final Map<String, List<DialogEvent>> dialogEvents = new HashMap<String, List<DialogEvent>>();
    private ActorRef cdrAggregator;

    public ProxyCallMonitor() {
    }

    @Override
    public void onReceive(Object message) throws Exception {
        SipReq req = (SipReq) message;
        String callId = req.getCallId();
        List<DialogEvent> events = dialogEvents.get(callId);
        if (events == null) {
            events = new ArrayList<DialogEvent>();
            dialogEvents.put(callId, events);
        }

        int factor = events.size() + 1;
        DialogEvent event = createDialogEvent(req, factor);

        events.add(event);

        logger.info(req.toString());

        if (req.isDone()) {
            publishDialogEvent(event);
            dialogEvents.remove(callId);
        }
    }

    private DialogEvent createDialogEvent(SipReq req, int factor) {
        return new DialogEvent(req.getCallId(), factor * req.getInc(), req.isDone());
    }

    private void publishDialogEvent(DialogEvent event) {
        logger.info("Publishing from ProxyCallMonitor:\t" + event.toString());

        if (cdrAggregator == null) {
            // TODO how to do this? actor id and registered id?
            // ActorRef[] matching =
            // ActorRegistry.actorsFor(SystemConfiguration.cdrAggregatorId);
            ActorRef[] matching = ActorRegistry.actorsFor(CdrAggregator.class.getName());
            if (matching.length > 0) {
                cdrAggregator = matching[0];
            }
        }

        if (cdrAggregator != null) {
            cdrAggregator.sendOneWay(event);
            logger.info("Published from ProxyCallMonitor:\t" + event.toString());
        }
    }
}
