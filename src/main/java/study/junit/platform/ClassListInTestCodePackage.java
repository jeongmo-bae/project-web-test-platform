package study.junit.platform;

import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

public class ClassListInTestCodePackage {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("testauto.testcode"))
                .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        for (TestIdentifier id : testPlan.getRoots()) {
            System.out.println("####1. Engine : " + id);
            testPlan.getChildren(id).forEach(child -> {
                System.out.println("####2. Test Program(Class) : " + child);
            });
        }

    }
}
