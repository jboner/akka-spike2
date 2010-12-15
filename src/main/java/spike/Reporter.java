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

    Reporter(String remoteHost, int remotePort) {
        getContext().makeRemote(remoteHost, remotePort);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        CdrEvent cdrEvent = (CdrEvent) message;
        logger.info(cdrEvent.toString());
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

    }
}
