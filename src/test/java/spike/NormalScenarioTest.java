package spike;

import static spike.TestHelper.compareFiles;
import static spike.TestHelper.startJVM;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NormalScenarioTest {

    List<Process> processes = new ArrayList<Process>();

    @Before
    public void setUp() throws Exception {
        Process servicenode1Process = startJVM(ServiceNode.class, "1");
        processes.add(servicenode1Process);
        Process reportProcess = startJVM(ReportNode.class, null);
        processes.add(reportProcess);
    }

    @After
    public void tearDown() {
        for (Process each : processes) {
            try {
                each.destroy();
            } catch (RuntimeException ignore) {
            }
        }
    }

    @Test
    public void testNormalCase() throws Exception {
        EdgeProxy producer = new EdgeProxy();
        producer.simulateLoad();

        Thread.sleep(5000);

        compareFiles(new File("./src/main/resources/cdr-reference.txt"), new File("./logs/cdr.txt"));
    }

}
