package spike;

public class SystemConfiguration {
    public static final RemoteLookupInfo proxyCallMonitor1Info = new RemoteLookupInfo("localhost", 2551,
            "proxyCallMonitor");
    public static final RemoteLookupInfo proxyCallMonitor2Info = new RemoteLookupInfo("localhost", 2552,
            "proxyCallMonitor");

    public static final RemoteLookupInfo[] proxyCallMonitorInfos = { proxyCallMonitor1Info, proxyCallMonitor2Info };

    public static final RemoteLookupInfo cdrAggregator1Info = new RemoteLookupInfo("localhost", 2551, "cdrAggregator");
    public static final RemoteLookupInfo cdrAggregator2Info = new RemoteLookupInfo("localhost", 2552, "cdrAggregator");

    public static final RemoteLookupInfo[] cdrAggregatorInfos = { cdrAggregator1Info, cdrAggregator2Info };

    public static final RemoteLookupInfo reporterInfo = new RemoteLookupInfo("localhost", 2553, "reporter");

    public static class RemoteLookupInfo {
        public final String host;
        public final int port;
        public final String id;

        public RemoteLookupInfo(String host, int port, String id) {
            this.host = host;
            this.port = port;
            this.id = id;
        }

    }

}
