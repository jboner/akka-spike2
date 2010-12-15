package spike;

import static spike.SystemConfiguration.servicenodeHost1;
import static spike.SystemConfiguration.servicenodeHost2;
import static spike.SystemConfiguration.servicenodePort1;
import static spike.SystemConfiguration.servicenodePort2;
import static spike.TestHelper.compareFiles;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * #1
 * 
 * <pre>
 * S1: o
 * S2: z
 * </pre>
 * 
 */
public class SingleJvmTwoServiceNodesTest {

    @Before
    public void setUp() throws Exception {
        ReportNode reportNode = new ReportNode();
        reportNode.start();
        ServiceNode serviceNode1 = new ServiceNode();
        serviceNode1.start(servicenodeHost1, servicenodePort1);
        ServiceNode serviceNode2 = new ServiceNode();
        serviceNode2.start(servicenodeHost2, servicenodePort2);
    }

    @Test
    public void testNormal() throws Exception {
        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad();

        Thread.sleep(2000);

        compareFiles(new File("./src/main/resources/cdr-reference.txt"), new File("./logs/cdr.txt"));
    }

}
