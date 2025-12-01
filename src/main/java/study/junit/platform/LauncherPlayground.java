
package study.junit.platform;

import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import testauto.testcode.unit.SampleTest;

import java.util.Arrays;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

public class LauncherPlayground {

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
//                        selectClass(SampleTest.class) // 방금 만든 테스트
                        selectMethod(SampleTest.class, "successTest"),
                        selectMethod(SampleTest.class, "failedTest"),
                        selectNestedClass(
                                Arrays.asList(SampleTest.class),   // enclosingClasses
                                SampleTest.NestedGroup.class       // nestedClass
                        )
                )
                .build();

        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();

            // 1) 테스트 플랜(트리) 먼저 뽑아보기
            TestPlan testPlan = launcher.discover(request);
            System.out.println("=== TestPlan Tree ===");
            testPlan.getRoots().forEach(root ->
                    printTree(testPlan, root, 0)
            );

            // 2) 리스너 붙여서 실제 실행
            launcher.registerTestExecutionListeners(new SimpleLoggingListener());
            System.out.println("\n=== Execute ===");
            launcher.execute(request);
        }
    }

    private static void printTree(TestPlan plan, TestIdentifier id, int depth) {
        System.out.println("  ".repeat(depth) + "- " + id.getDisplayName() + " [" + id.getType() + "]");
        plan.getChildren(id).forEach(child -> printTree(plan, child, depth + 1));
    }
}