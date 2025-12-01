package testauto.service;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TestDiscoveryServiceImpl implements TestDiscoveryService{
    public static void main(String[] args) {
        (new TestDiscoveryServiceImpl()).discoverAllTests();
    }

    @Override
    public void discoverAllTests() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(
                        DiscoverySelectors.selectPackage("testauto.testcode")
                )
                .build();
        TestPlan testPlan = LauncherFactory.create()
                .discover(request);

        System.out.println("testPlan.getRoots().size() = " + testPlan.getRoots().size());
        Set<TestIdentifier> identifiers = testPlan.getRoots();
        System.out.println("identifiers = " + identifiers);

        for (TestIdentifier id : identifiers) {
            UniqueId rootUniqueId = id.getUniqueIdObject();
            System.out.println("rootUniqueId = " + rootUniqueId);
            Set<TestIdentifier> children = testPlan.getChildren(id);
            children.forEach(child -> {
                System.out.println("=================================================");
                System.out.println("child = " + child);
                System.out.println("child.getUniqueId() = " + child.getUniqueId());
                System.out.println("child.getDisplayName() = " + child.getDisplayName());
                System.out.println("child.getParentIdObject() = " + child.getParentIdObject());
                System.out.println("child.getType() = " + child.getType());
                System.out.println("child.isTest() = " + child.isTest());
                System.out.println("child.getSource() = " + child.getSource());
                System.out.println("child.getSource().orElse(null) = " + child.getSource().orElse(null));
                ClassSource testSource = (ClassSource) child.getSource().orElse(null);
                System.out.println(testSource.getClassName());
                System.out.println("=================================================");
            });
        }

    }

    @Override
    public void discoverTests(TestIdentifier id) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(
                        DiscoverySelectors.selectPackage("testauto.testcode")
                )
                .build();
    }

}
