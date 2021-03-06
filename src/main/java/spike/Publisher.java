package spike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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
        if (!isPrimaryNode) {
            removePrimaryNodeSubscriptions();
        }
    }

    public void addSubscriber(ActorRef subscriber, Subscribe subscribeEvent) {
        if (!hasSubscriber(subscriber, subscribeEvent.getType())) {
            if (isPrimaryNode || subscribeEvent.getType() == Subscribe.Type.NORMAL) {
                logger.info("Added subscriber {} {}", subscriber.getId(), subscribeEvent.getType());
                List<ActorRef> subscribersForType = subscribers.get(subscribeEvent.getType());
                subscribersForType.add(subscriber);
            }
        }
    }

    private boolean hasSubscriber(ActorRef subscriber, Subscribe.Type type) {
        List<ActorRef> subscribersForType = subscribers.get(type);
        // equals is not based on id
        for (ActorRef each : subscribersForType) {
            if (each.getId().equals(subscriber.getId())) {
                return true;
            }
        }
        return false;
    }

    private void removePrimaryNodeSubscriptions() {
        subscribers.get(Subscribe.Type.BUDDY).clear();
        subscribers.get(Subscribe.Type.PRIMARY_ONLY).clear();
    }

    public void removeSubscriber(ActorRef subscriber, Unsubscribe unsubscribeEvent) {
        List<ActorRef> subscribersForType = subscribers.get(unsubscribeEvent.getType());
        // equals is not based on id
        ListIterator<ActorRef> iter = subscribersForType.listIterator();
        while (iter.hasNext()) {
            ActorRef each = iter.next();
            if (each.getId().equals(subscriber.getId())) {
                iter.remove();
            }
        }
    }

    public void publish(Object event) {
        publish(event, subscribers.get(Subscribe.Type.BUDDY));
        publish(event, subscribers.get(Subscribe.Type.PRIMARY_ONLY));
        publish(event, subscribers.get(Subscribe.Type.NORMAL));
    }

    public void publishSnapshot(Object event, ActorRef subscriber, Subscribe.Type subscriptionType) {
        if (hasSubscriber(subscriber, subscriptionType)) {
            publish(event, subscriber);
        }
    }

    private void publish(Object event, List<ActorRef> toSubscribers) {
        for (ActorRef subscriber : toSubscribers) {
            publish(event, subscriber);
        }
    }

    private void publish(Object event, ActorRef subscriber) {
        try {
            subscriber.sendOneWay(event);
            logger.info("Published: {}", event);
        } catch (RuntimeException e) {
            logger.info("Failed to publish: {}", event);
        }
    }

}
