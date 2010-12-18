package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
public class SingleJvmLateStartOfNode2Failover2Test extends SingleJvmTest {

    @Override
    protected boolean isServiceNode2ToBeStarted() {
        return false;
    }

    @Test
    public void testFailover() throws Exception {
        startServiceNode2After(100, TimeUnit.MILLISECONDS);
        killServiceNode1After(2000, TimeUnit.MILLISECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, SECONDS);

        sleep(2000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }
}
