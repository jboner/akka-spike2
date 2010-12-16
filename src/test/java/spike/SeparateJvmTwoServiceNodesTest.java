package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;

import java.io.File;

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
public class SeparateJvmTwoServiceNodesTest extends SeparateJvmTest {

    @Test
    public void testNormalCase() throws Exception {
        EdgeProxy producer = new EdgeProxy();
        producer.simulateLoad(1000, 5, SECONDS);

        Thread.sleep(1000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }

}
