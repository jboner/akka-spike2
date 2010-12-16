package spike;

import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;

import org.junit.After;
import org.junit.Before;

public abstract class SingleJvmTest {

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

}
