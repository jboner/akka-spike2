package spike;

import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

public abstract class SingleJvmTest {
	protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	protected ReportNode reportNode;
	protected ServiceNode serviceNode1;
	protected ServiceNode serviceNode2;

	@Before
	public void setUp() throws Exception {
		reportNode = new ReportNode();
		reportNode.start();
		if (isServiceNode1ToBeStarted()) {
			serviceNode1 = new ServiceNode();
			serviceNode1.start(servicenodeHost1, servicenodePort1);
		}
		if (isServiceNode2ToBeStarted()) {
			serviceNode2 = new ServiceNode();
			serviceNode2.start(servicenodeHost2, servicenodePort2);
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

}
