package spike;

import static akka.actor.UntypedActor.actorOf;
import static spike.SystemConfiguration.reporterId;
import static spike.SystemConfiguration.reportnodeHost;
import static spike.SystemConfiguration.reportnodePort;
import akka.actor.ActorRef;
import akka.remote.RemoteServer;

public class ReportNode {

    private RemoteServer reportnode;
    private ActorRef reporter;

    public static void main(String[] args) {
        ReportNode manager = new ReportNode();
        manager.start();
    }

    public void start() {
        reportnode = new RemoteServer();
        reportnode.start(reportnodeHost, reportnodePort);
        reporter = actorOf(Reporter.class);
        reportnode.register(reporterId, reporter);
    }

    public void stop() {
        reporter.stop();
        reportnode.shutdown();
    }

}
