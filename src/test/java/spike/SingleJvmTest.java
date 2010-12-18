package spike;

import static spike.TestHelper.sleep;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import akka.actor.ActorRegistry;

public abstract class SingleJvmTest {
    protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected ReportNode reportNode;
    protected ServiceNode serviceNode1;
    protected ServiceNode serviceNode2;

    @Before
    public void setUp() throws Exception {
        if (isServiceNode1ToBeStarted()) {
            serviceNode1 = new ServiceNode(0);
            serviceNode1.start();
        }
        if (isServiceNode2ToBeStarted()) {
            serviceNode2 = new ServiceNode(1);
            serviceNode2.start();
        }
        // TODO test starting ReporterNode before ServiceNode
        reportNode = new ReportNode();
        reportNode.start();

        // TODO we should not need this sleep, right now it is needed for all
        // subscriptions to be initialized
        sleep(7000);

        File resultFile = resultFile();
        if (resultFile.exists()) {
            resultFile.delete();
        }
    }

    protected boolean isServiceNode1ToBeStarted() {
        return true;
    }

    protected boolean isServiceNode2ToBeStarted() {
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
                serviceNode1.start();
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
                serviceNode2.start();
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

    protected File resultFile() {
        return new File("./logs/cdr.txt");
    }

}
