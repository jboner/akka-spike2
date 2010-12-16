package spike;

import static spike.TestHelper.compareFiles;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
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

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
        super.tearDown();
    }

    @Test
    public void testNormal() throws Exception {
        Runnable killServiceNode1 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                serviceNode1.stop();
            }
        };
        executor.schedule(killServiceNode1, 2, TimeUnit.SECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, TimeUnit.SECONDS);

        Thread.sleep(1000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }
}
