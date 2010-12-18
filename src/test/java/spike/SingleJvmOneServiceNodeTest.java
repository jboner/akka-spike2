package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import java.io.File;

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
public class SingleJvmOneServiceNodeTest extends SingleJvmTest {

    @Override
    protected boolean isServiceNode2ToBeStarted() {
        return false;
    }

    @Test
    public void testNormal() throws Exception {
        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, SECONDS);

        sleep(1000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), resultFile());
    }

}
