package spike;

import java.io.Serializable;

public class Subscribe implements Serializable {
    private static final long serialVersionUID = 5716177789671273940L;

    public enum Type {
        BUDDY, NORMAL, PRIMARY_ONLY
    }

    private final Type type;
    private final long fromEtag;
    private final boolean replyImmediatly;

    public Subscribe(Type type, long fromEtag, boolean replyImmediately) {
        this.type = type;
        this.fromEtag = fromEtag;
        replyImmediatly = replyImmediately;
    }

    public long getFromEtag() {
        return fromEtag;
    }

    public Type getType() {
        return type;
    }

    public boolean isReplyImmediatly() {
        return replyImmediatly;
    }

    @Override
    public String toString() {
        return "Subscribe fromEtag " + fromEtag + " " + type + (replyImmediatly ? " ReplyImmediatly" : "");
    }

}
