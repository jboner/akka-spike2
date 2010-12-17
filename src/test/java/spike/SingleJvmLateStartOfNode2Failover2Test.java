package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort2;
import static spike.TestHelper.compareFiles;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

/**
 * Test case #3
 * 
 * <pre>
 * S1: o o x
 * S2: x z o
 * </pre>
 * 
 */
public class SingleJvmLateStartOfNode2Failover2Test extends SingleJvmTest {

	ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@Override
	protected boolean isServiceNode2ToBeStarted() {
		return false;
	}

	@After
	public void shutDownExecutor() {
		executor.shutdownNow();
	}

	@Test
	public void testNormal() throws Exception {
		Runnable startServiceNode2 = new Runnable() {
			@Override
			public void run() {
				if (executor.isShutdown()) {
					return;
				}
				ServiceNode serviceNode2 = new ServiceNode();
				serviceNode2.start(servicenodeHost2, servicenodePort2);
			}
		};
		Runnable killServiceNode1 = new Runnable() {
			@Override
			public void run() {
				if (executor.isShutdown()) {
					return;
				}
				serviceNode1.stop();
			}
		};

		executor.schedule(startServiceNode2, 100, TimeUnit.MILLISECONDS);
		executor.schedule(killServiceNode1, 1500, TimeUnit.MILLISECONDS);

		EdgeProxy edgeProxy = new EdgeProxy();
		edgeProxy.simulateLoad(1000, 5, SECONDS);

		Thread.sleep(2000);

		compareFiles(new File("./src/main/resources/cdr-reference-1000.txt"), new File("./logs/cdr.txt"));
	}
}
