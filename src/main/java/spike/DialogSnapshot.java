package spike;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogSnapshot implements Serializable {
    private final long etag;
    private final List<DialogEvent> events;

    public DialogSnapshot(long etag, List<DialogEvent> events) {
        this.etag = etag;
        this.events = new ArrayList<DialogEvent>(events);
    }

    public long getEtag() {
        return etag;
    }

    public List<DialogEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @Override
    public String toString() {
        return "DialogSnapshot up to etag: " + etag + " containing " + events.size() + " events";
    }

}
