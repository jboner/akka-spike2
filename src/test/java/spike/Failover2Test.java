package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import org.junit.Test;

/**
 * Test case #2
 * 
 * <pre>
 * S1: o x
 * S2: z o
 * </pre>
 * 
 */
public class Failover2Test extends Base {

    @Test
    public void testFailover() throws Exception {
        killServiceNode1After(4, SECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 10, SECONDS);

        sleep(1000);

        compareFiles(referenceFile(1000), resultFile());
    }

}
