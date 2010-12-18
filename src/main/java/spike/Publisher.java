package spike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import akka.actor.ActorRef;

public class Publisher {

    private final Map<Subscribe.Type, List<ActorRef>> subscribers = new HashMap<Subscribe.Type, List<ActorRef>>();
    private boolean isPrimaryNode;
    private final Logger logger;

    public Publisher(Logger logger) {
        this.logger = logger;
        for (Subscribe.Type each : Subscribe.Type.values()) {
            subscribers.put(each, new ArrayList<ActorRef>());
        }
    }

    public boolean isPrimaryNode() {
        return isPrimaryNode;
    }

    public void setPrimaryNode(boolean isPrimaryNode) {
        this.isPrimaryNode = isPrimaryNode;
    }

    public void addSubscriber(ActorRef subscriber, Subscribe subscribeEvent) {
        List<ActorRef> subscribersForType = subscribers.get(subscribeEvent.getType());
        if (!subscribersForType.contains(subscriber)) {
            logger.info("Added subscriber {} {}", subscriber.getId(), subscribeEvent.getType());
            subscribersForType.add(subscriber);
        }
    }

    public void removeSubscriber(ActorRef subscriber, Unsubscribe unsubscribeEvent) {
        subscribers.get(unsubscribeEvent.getType()).remove(subscriber);
    }

    public void publish(Object event) {
        if (isPrimaryNode) {
            publish(event, subscribers.get(Subscribe.Type.BUDDY));
            publish(event, subscribers.get(Subscribe.Type.PRIMARY_ONLY));
        }

        publish(event, subscribers.get(Subscribe.Type.NORMAL));
    }

    private void publish(Object event, List<ActorRef> toSubscribers) {
        for (ActorRef subscriber : toSubscribers) {
            try {
                subscriber.sendOneWay(event);
                logger.info("Published: {}", event);
            } catch (RuntimeException e) {
                logger.info("Failed to publish: {}", event);
            }
        }
    }

}
