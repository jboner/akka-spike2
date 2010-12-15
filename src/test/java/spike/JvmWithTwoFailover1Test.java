package spike;

import static spike.TestHelper.compareFiles;
import static spike.TestHelper.startJVM;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JvmWithTwoFailover1Test {

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    List<Process> processes = new ArrayList<Process>();
    private Process servicenode1Process;

    @Before
    public void setUp() throws Exception {
        Process reportProcess = startJVM(ReportNode.class, null);
        processes.add(reportProcess);
        servicenode1Process = startJVM(ServiceNode.class, "1");
        processes.add(servicenode1Process);
        Process servicenode2Process = startJVM(ServiceNode.class, "2");
        processes.add(servicenode2Process);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        for (Process each : processes) {
            try {
                each.destroy();
            } catch (RuntimeException ignore) {
            }
        }

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
        executor.schedule(killServiceNode1, 2, TimeUnit.SECONDS);

        EdgeProxy producer = new EdgeProxy();
        producer.simulateLoad();

        Thread.sleep(5000);

        compareFiles(new File("./src/main/resources/cdr-reference.txt"), new File("./logs/cdr.txt"));
    }

}
