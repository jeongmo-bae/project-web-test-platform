package testauto.service;

import org.junit.platform.launcher.TestIdentifier;

public interface TestDiscoveryService {
    void discoverAllTests();
    void discoverTests(TestIdentifier id);
}
