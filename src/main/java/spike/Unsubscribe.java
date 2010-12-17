package spike;

import java.io.Serializable;

import spike.Subscribe.Type;

public class Unsubscribe implements Serializable {
    private static final long serialVersionUID = 5716177789671273940L;

    private final Type type;

    public Unsubscribe(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

}
