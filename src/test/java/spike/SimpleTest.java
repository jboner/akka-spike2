package spike;

import static spike.TestHelper.compareFiles;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class SimpleTest {

    @Before
    public void setUp() throws Exception {
        ReportNode reportNode = new ReportNode();
        reportNode.start();
        ServiceNode serviceNode = new ServiceNode();
        serviceNode.start();
    }

    @Test
    public void testNormal() throws Exception {
        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad();

        Thread.sleep(2000);

        compareFiles(new File("./src/main/resources/cdr-reference.txt"), new File("./logs/cdr.txt"));
    }

}
