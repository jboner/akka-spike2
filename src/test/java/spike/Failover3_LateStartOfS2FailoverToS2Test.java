package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import org.junit.Test;

/**
 * Test case #3
 * 
 * <pre>
 * S1: o o x
 * S2: x z o
 * </pre>
 * 
 */
public class Failover3_LateStartOfS2FailoverToS2Test extends Base {

    @Override
    protected boolean isServiceNode2ToBeStarted() {
        return false;
    }

    @Test
    public void testFailover() throws Exception {
        startServiceNode2After(4, SECONDS);
        killServiceNode1After(15, SECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 20, SECONDS);

        sleep(2, SECONDS);

        compareFiles(referenceFile(1000), resultFile());
    }
}
