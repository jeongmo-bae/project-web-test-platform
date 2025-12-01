package study.junit.platform;

import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

public class ClassListInTestCodePackage {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("testauto.testcode.unit"))
//                .selectors(selectClass(testauto.testcode.unit.SampleTest.class))
                .build();
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            TestPlan testPlan = launcher.discover(request);
            System.out.println("\n>>>" + testPlan);
            for (TestIdentifier id : testPlan.getRoots()) {
//                System.out.println(id);
                getChild(testPlan, id, 1);
            }


//            System.out.println("=== TestPlan Tree ===");
//            testPlan.getRoots().forEach(root ->
//                    printTree(testPlan, root, 0)
//            );
        }
    }

    private static void getChild(TestPlan plan, TestIdentifier id, int depth) {
        plan.getChildren(id).forEach(child -> System.out.println("  ".repeat(depth) + "- " + child.getDisplayName() + " [" + child.getType() + "]"));

    }
    private static void printTree(TestPlan plan, TestIdentifier id, int depth) {
        System.out.println("  ".repeat(depth) + "- " + id.getDisplayName() + " [" + id.getType() + "]");
        plan.getChildren(id).forEach(child -> printTree(plan, child, depth + 1));
    }
}
