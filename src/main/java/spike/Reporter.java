package spike;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

class Reporter extends UntypedActor {

    private static Logger logger = LoggerFactory.getLogger(Reporter.class);
    private boolean firstTime = true;

    Reporter() {
    }

    @Override
    public void onReceive(Object message) throws Exception {
        try {
            CdrEvent cdrEvent = (CdrEvent) message;
            logger.info("Recieved in Reporter: " + cdrEvent.toString() + " first: " + firstTime);
            BufferedWriter writer = null;
            try {
                boolean append = !firstTime;
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./logs/cdr" + ".txt", append),
                        "UTF-8"));
                writer.write(cdrEvent.toString());
                writer.write("\n");
                firstTime = false;
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
}
