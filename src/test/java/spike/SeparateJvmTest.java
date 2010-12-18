package spike;

import static spike.TestHelper.startJVM;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

public abstract class SeparateJvmTest {

    protected List<Process> processes = new ArrayList<Process>();
    protected Process servicenode1Process;
    protected Process servicenode2Process;
    protected Process reportProcess;

    @Before
    public void setUp() throws Exception {
        if (isServiceNode1ToBeStarted()) {
            servicenode1Process = startJVM(ServiceNode.class, "0");
            processes.add(servicenode1Process);
        }
        if (isServiceNode2ToBeStarted()) {
            servicenode2Process = startJVM(ServiceNode.class, "1");
            processes.add(servicenode2Process);
        }
        reportProcess = startJVM(ReportNode.class, null);
        processes.add(reportProcess);

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
    public void tearDown() {
        for (Process each : processes) {
            try {
                each.destroy();
            } catch (RuntimeException ignore) {
            }
        }
    }

    protected File resultFile() {
        return new File("./logs/cdr.txt");
    }
}
