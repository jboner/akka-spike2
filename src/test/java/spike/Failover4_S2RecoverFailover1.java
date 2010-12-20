package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import org.junit.Test;

/**
 * Test case #4
 * 
 * <pre>
 * S1: o x z o
 * S2: z o o x
 * </pre>
 * 
 */
public class Failover4_S2RecoverFailover1 extends Base {

    @Test
    public void testFailover() throws Exception {
        killServiceNode1After(3, SECONDS);
        startServiceNode1After(15, SECONDS);
        killServiceNode2After(26, SECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 30, SECONDS);

        sleep(2, SECONDS);

        compareFiles(referenceFile(1000), resultFile());
    }

}
