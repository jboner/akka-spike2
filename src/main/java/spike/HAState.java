package spike;

import java.io.Serializable;

public class HAState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean isPrimaryNode;

    public HAState(boolean isPrimaryNode) {
        this.isPrimaryNode = isPrimaryNode;
    }

    public boolean isPrimaryNode() {
        return isPrimaryNode;
    }

    @Override
    public String toString() {
        return "HAState primary: " + isPrimaryNode;
    }

}
