package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
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

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @After
    public void shutDownExecutor() {
        executor.shutdownNow();
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
        Runnable startServiceNode1 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                serviceNode1.start();
            }
        };
        Runnable killServiceNode2 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                serviceNode2.stop();
            }
        };

        executor.schedule(killServiceNode1, 500, TimeUnit.MILLISECONDS);
        executor.schedule(startServiceNode1, 1000, TimeUnit.MILLISECONDS);
        executor.schedule(killServiceNode2, 1500, TimeUnit.MILLISECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, SECONDS);

        Thread.sleep(2000);

        compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
    }
}
