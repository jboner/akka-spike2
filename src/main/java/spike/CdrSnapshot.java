package spike;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CdrSnapshot implements Serializable {
    private final long etag;
    private final List<CdrEvent> events;

    public CdrSnapshot(long etag, List<CdrEvent> events) {
        this.etag = etag;
        this.events = new ArrayList<CdrEvent>(events);
    }

    public long getEtag() {
        return etag;
    }

    public List<CdrEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @Override
    public String toString() {
        return "CdrSnapshot up to etag: " + etag + " containing " + events.size() + " events";
    }

}
