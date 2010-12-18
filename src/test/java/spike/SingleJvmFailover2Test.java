package spike;

import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
public class SingleJvmFailover2Test extends SingleJvmTest {

    @Test
    public void testFailover() throws Exception {
        killServiceNode1After(2, TimeUnit.SECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, TimeUnit.SECONDS);

        sleep(1000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), resultFile());
    }

}
