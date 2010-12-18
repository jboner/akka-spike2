package spike;

import java.io.Serializable;

public class Subscribe implements Serializable {
    private static final long serialVersionUID = 5716177789671273940L;

    public enum Type {
        BUDDY, NORMAL, PRIMARY_ONLY
    }

    private final Type type;
    private final long fromEtag;

    public Subscribe(Type type, long fromEtag) {
        this.type = type;
        this.fromEtag = fromEtag;
    }

    public long getFromEtag() {
        return fromEtag;
    }

    public Type getType() {
        return type;
    }

}
