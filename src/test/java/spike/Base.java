package spike;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static spike.TestHelper.sleep;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import akka.actor.ActorRegistry;

public abstract class Base {
    protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected ReportNode reportNode;
    protected ServiceNode serviceNode1;
    protected ServiceNode serviceNode2;

    @Before
    public void setUp() throws Exception {
        if (isServiceNode1ToBeStarted()) {
            startServiceNode1();
        }
        if (isServiceNode2ToBeStarted()) {
            startServiceNode2();
        }
        if (isReporterNodeToBeStarted()) {
            startReportNode();
        }

        sleep(1100, MILLISECONDS);

        File resultFile = resultFile();
        if (resultFile.exists()) {
            resultFile.delete();
        }
    }

    private void startServiceNode1() {
        serviceNode1 = new ServiceNode(0);
        serviceNode1.start();
    }

    private void startServiceNode2() {
        serviceNode2 = new ServiceNode(1);
        serviceNode2.start();
    }

    private void startReportNode() {
        reportNode = new ReportNode();
        reportNode.start();
    }

    protected boolean isServiceNode1ToBeStarted() {
        return true;
    }

    protected boolean isServiceNode2ToBeStarted() {
        return true;
    }

    protected boolean isReporterNodeToBeStarted() {
        return true;
    }

    @After
    public void tearDown() throws Exception {
        if (reportNode != null) {
            reportNode.stop();
        }
        if (serviceNode1 != null) {
            serviceNode1.stop();
        }
        if (serviceNode2 != null) {
            serviceNode2.stop();
        }
        ActorRegistry.shutdownAll();
    }

    @After
    public void shutDownExecutor() {
        executor.shutdownNow();
    }

    protected void killServiceNode1After(long delay, TimeUnit timeUnit) {
        Runnable killServiceNode1 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                serviceNode1.stop();
            }
        };
        executor.schedule(killServiceNode1, delay, timeUnit);
    }

    protected void startServiceNode1After(long delay, TimeUnit timeUnit) {

        Runnable startServiceNode1 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                startServiceNode1();
            }
        };
        executor.schedule(startServiceNode1, delay, timeUnit);
    }

    protected void startServiceNode2After(long delay, TimeUnit timeUnit) {
        Runnable startServiceNode2 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                startServiceNode2();
            }
        };
        executor.schedule(startServiceNode2, delay, timeUnit);
    }

    protected void killServiceNode2After(long delay, TimeUnit timeUnit) {
        Runnable killServiceNode2 = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                serviceNode2.stop();
            }
        };
        executor.schedule(killServiceNode2, delay, timeUnit);
    }

    protected void startReportNodeAfter(long delay, TimeUnit timeUnit) {
        Runnable startReportNode = new Runnable() {
            @Override
            public void run() {
                if (executor.isShutdown()) {
                    return;
                }
                startReportNode();
            }
        };
        executor.schedule(startReportNode, delay, timeUnit);
    }

    protected File resultFile() {
        return new File("./logs/cdr.txt");
    }

    protected File referenceFile(int n) {
        return new File("./src/main/resources/cdr-reference-" + n + ".txt");
    }

}
