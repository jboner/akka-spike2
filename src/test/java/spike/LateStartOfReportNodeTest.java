package spike;

import static java.util.concurrent.TimeUnit.SECONDS;
import static spike.TestHelper.compareFiles;
import static spike.TestHelper.sleep;

import org.junit.Test;

/**
 * Start ReporterNode after a while, i.e. it needs snapshot of previous
 * CdrEvents.
 * 
 */
public class LateStartOfReportNodeTest extends Base {

    @Override
    protected boolean isReporterNodeToBeStarted() {
        return false;
    }

    @Test
    public void testNormal() throws Exception {
        startReportNodeAfter(2, SECONDS);
        EdgeProxy edgeProxy = new EdgeProxy();
        edgeProxy.simulateLoad(1000, 5, SECONDS);

        sleep(1000);

        compareFiles(referenceFile(1000), resultFile());
    }

}
