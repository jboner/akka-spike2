package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import org.junit.Test;

/**
 * Test case #0
 * 
 * <pre>
 * S1: o
 * S2: x
 * </pre>
 * 
 */
public class OneServiceNodeTest extends Base {

    @Override
    protected boolean isServiceNode2ToBeStarted() {
        return false;
    }

    @Test
    public void testNormal() throws Exception {
        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, SECONDS);

        sleep(1000);

        compareFiles(referenceFile(1000), resultFile());
    }

}
