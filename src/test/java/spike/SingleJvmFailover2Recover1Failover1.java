package spike;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import java.io.File;

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
public class SingleJvmFailover2Recover1Failover1 extends SingleJvmTest {

    @Test
    public void testFailover() throws Exception {
        killServiceNode1After(500, MILLISECONDS);
        startServiceNode1After(1000, MILLISECONDS);
        killServiceNode2After(1500, MILLISECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, SECONDS);

        sleep(2000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }
}
