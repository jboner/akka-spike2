package spike;

public class SystemConfiguration {
    public static final RemoteLookupInfo proxyCallMonitor1Info = new RemoteLookupInfo("localhost", 2551,
            ProxyCallMonitor.class.getName() + "-1");
    public static final RemoteLookupInfo proxyCallMonitor2Info = new RemoteLookupInfo("localhost", 2552,
            ProxyCallMonitor.class.getName() + "-2");

    public static final RemoteLookupInfo[] proxyCallMonitorInfos = { proxyCallMonitor1Info, proxyCallMonitor2Info };

    public static final RemoteLookupInfo cdrAggregator1Info = new RemoteLookupInfo("localhost", 2551,
            CdrAggregator.class.getName() + "-1");
    public static final RemoteLookupInfo cdrAggregator2Info = new RemoteLookupInfo("localhost", 2552,
            CdrAggregator.class.getName() + "-2");

    public static final RemoteLookupInfo[] cdrAggregatorInfos = { cdrAggregator1Info, cdrAggregator2Info };

    public static final RemoteLookupInfo reporterInfo = new RemoteLookupInfo("localhost", 2553,
            Reporter.class.getName() + "-1");

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
