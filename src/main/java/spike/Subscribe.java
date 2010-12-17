package spike;

import java.io.Serializable;

public class Subscribe implements Serializable {
    private static final long serialVersionUID = 5716177789671273940L;

    public enum Type {
        BUDDY, NORMAL, PRIMARY_ONLY
    }

    private final Type type;
    private final String fromEtag;

    public Subscribe(Type type, String fromEtag) {
        this.type = type;
        this.fromEtag = fromEtag;
    }

    public String getFromEtag() {
        return fromEtag;
    }

    public Type getType() {
        return type;
    }

}
