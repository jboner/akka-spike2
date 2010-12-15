package spike;

import static akka.actor.UntypedActor.actorOf;
import static spike.SystemConfiguration.reporterId;
import static spike.SystemConfiguration.reportnodeHost;
import static spike.SystemConfiguration.reportnodePort;
import akka.remote.RemoteServer;

public class ReportNode {

    private RemoteServer reportnode;

    public static void main(String[] args) {
        ReportNode manager = new ReportNode();
        manager.start();
    }

    public void start() {
        reportnode = new RemoteServer();
        reportnode.start(reportnodeHost, reportnodePort);
        reportnode.register(reporterId, actorOf(Reporter.class));
    }

}
