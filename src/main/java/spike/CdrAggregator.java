package spike;

import static spike.SystemConfiguration.reporterId;
import static spike.SystemConfiguration.reportnodeHost;
import static spike.SystemConfiguration.reportnodePort;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.remote.RemoteClient;

public class CdrAggregator extends UntypedActor {

    private static Logger logger = LoggerFactory.getLogger(CdrAggregator.class);

    private final Map<String, Long> currentState = new HashMap<String, Long>();
    private ActorRef reporterActor;

    public CdrAggregator() {
    }

    @Override
    public void onReceive(Object message) throws Exception {
        DialogEvent event = (DialogEvent) message;
        apply(event);
    }

    private void apply(DialogEvent event) {
        String callId = event.getCallId();
        Long value = currentState.get(callId);
        if (value == null) {
            value = 0L;
        }
        value += event.getInc();
        currentState.put(callId, value);

        logger.info(event.toString());

        if (event.isDone()) {
            publishCdrEvent(callId);
            currentState.remove(callId);
        }
    }

    private void publishCdrEvent(String callId) {
        Long sum = currentState.get(callId);
        if (sum == null) {
            throw new IllegalStateException("No such dialog: " + callId);
        }
        CdrEvent cdrEvent = new CdrEvent(callId, sum);

        logger.info("Publishing from CdrAggregator:\t" + cdrEvent.toString());

        if (reporterActor == null) {
            // TODO use registry instead? shouldn't have to know host/port
            reporterActor = RemoteClient.actorFor(reporterId, reportnodeHost, reportnodePort);
        }

        reporterActor.sendOneWay(cdrEvent);

        logger.info("Sent:\t" + cdrEvent.toString());

    }

}
