package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test case #1
 * 
 * <pre>
 * S1: o
 * S2: z
 * </pre>
 * 
 */
@Ignore
public class SeparateJvmTwoServiceNodesTest extends SeparateJvmTest {

    @Test
    public void testNormalCase() throws Exception {
        EdgeProxy producer = new EdgeProxy();
        producer.simulateLoad(1000, 5, SECONDS);

        sleep(1000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }

}
