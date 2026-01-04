package study.junit.platform;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

public class TestPlanPlayground {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
//                        selectMethod(DummyCalcTest.class, "add_test")
//                        selectMethod(SampleTest.class, "successTest")
                        selectClass("testauto.testcode.unit.DummyCalcTest")
                )
                .build();
        try (LauncherSession session = LauncherFactory.openSession()){
            Launcher launcher = session.getLauncher();
            TestPlan testPlan = launcher.discover(request);
            System.out.println("=== TestPlan.getRoots ===");
            testPlan.getRoots()
                    .forEach(
                    root -> { 
                        System.out.println("root = " +root);
                        printTree(testPlan, root.getUniqueIdObject());
            });
        }
    }
    private static void printTree(TestPlan testPlan, UniqueId parent){
        testPlan.getChildren(parent).forEach(child -> {
            System.out.println("child = " + child);
            printTree(testPlan, child.getUniqueIdObject());
        });
    }
}
