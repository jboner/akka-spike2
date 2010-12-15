package spike;

import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;
import static spike.TestHelper.compareFiles;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimpleWithTwoFailover1Test {

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ServiceNode serviceNode1;

    @Before
    public void setUp() throws Exception {
        ReportNode reportNode = new ReportNode();
        reportNode.start();
        serviceNode1 = new ServiceNode();
        serviceNode1.start(servicenodeHost1, servicenodePort1);
        ServiceNode serviceNode2 = new ServiceNode();
        serviceNode2.start(servicenodeHost2, servicenodePort2);
    }

    @After
    public void tearDown() {
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
        executor.schedule(killServiceNode1, 1, TimeUnit.SECONDS);

        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad();

        Thread.sleep(2000);

        compareFiles(new File("./src/main/resources/cdr-reference.txt"), new File("./logs/cdr.txt"));
    }
}
