package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Ignore;
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
@Ignore
public class SeparateJvmFailover2Test extends SeparateJvmTest {

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    @After
    public void tearDown() {
        executor.shutdownNow();
        super.tearDown();
    }

    @Test
    public void testFailServiceNode1() throws Exception {
        Runnable killServiceNode1 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                servicenode1Process.destroy();
            }
        };
        executor.schedule(killServiceNode1, 2, SECONDS);

        EdgeProxy producer = new EdgeProxy();
        producer.simulateLoad(1000, 5, SECONDS);

        Thread.sleep(1000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }

}
