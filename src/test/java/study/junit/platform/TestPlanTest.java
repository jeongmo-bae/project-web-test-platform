package study.junit.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

public class TestPlanTest {
    @Nested
    class AboutTestMethodSource{
        private TestPlan testPlan;
        @BeforeEach
        void setUp(){
            System.out.println("AboutTestMethodSource");
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectUniqueId("[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]"))
                    .build();
            testPlan = LauncherFactory.create().discover(request);
        }

        @Test
        void getTestPlan(){
            testPlan.getRoots()
                    .forEach(this::printTestPlan);
            testPlan.getChildren("[engine:junit-jupiter]")
                    .forEach(this::printTestPlan);
            testPlan.getChildren("[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]")
                    .forEach(this::printTestPlan);
            testPlan.getChildren("[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[nested-class:NestedGroup]")
                    .forEach(this::printTestPlan);
            /*
            AboutTestMethodSource
            id.getUniqueId() = [engine:junit-jupiter]
                id.getParentId() = Optional.empty
                id.getDisplayName() = JUnit Jupiter
                id.getType().name() = CONTAINER
                id.getLegacyReportingName() = JUnit Jupiter
                id.getSource() = Optional.empty
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]
                id.getParentId() = Optional[[engine:junit-jupiter]]
                id.getDisplayName() = 이건 Rebm 테스트 클래스로
             고객군 검증을 할거야
             마ㅓㄴ오마너옴
                id.getType().name() = CONTAINER
                id.getLegacyReportingName() = testauto.testcode.e2e.rebm.RebmCustGroupTest
                id.getSource() = Optional[ClassSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest', filePosition = null]]
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[method:successTest()]
                id.getParentId() = Optional[[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]]
                id.getDisplayName() = 플랫폼 개발용 테스트 메서드 입니다~ 성공테스트~~
                id.getType().name() = TEST
                id.getLegacyReportingName() = successTest()
                id.getSource() = Optional[MethodSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest', methodName = 'successTest', methodParameterTypes = '']]
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[method:exceptionTest()]
                id.getParentId() = Optional[[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]]
                id.getDisplayName() = 플랫폼 개발용이요~ 예외정상 발생 테스트 예시요~~
                id.getType().name() = TEST
                id.getLegacyReportingName() = exceptionTest()
                id.getSource() = Optional[MethodSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest', methodName = 'exceptionTest', methodParameterTypes = '']]
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[method:failTest()]
                id.getParentId() = Optional[[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]]
                id.getDisplayName() = 플랫폼 개발용 테스트 메서드 입니다! 실패 테스트~~
                id.getType().name() = TEST
                id.getLegacyReportingName() = failTest()
                id.getSource() = Optional[MethodSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest', methodName = 'failTest', methodParameterTypes = '']]
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[nested-class:NestedGroup]
                id.getParentId() = Optional[[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]]
                id.getDisplayName() = NestedGroup
                id.getType().name() = CONTAINER
                id.getLegacyReportingName() = testauto.testcode.e2e.rebm.RebmCustGroupTest$NestedGroup
                id.getSource() = Optional[ClassSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest$NestedGroup', filePosition = null]]
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[nested-class:NestedGroup]/[method:nestedTest2()]
                id.getParentId() = Optional[[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[nested-class:NestedGroup]]
                id.getDisplayName() = 네스티드 내에 정의된 메서드 2~
                id.getType().name() = TEST
                id.getLegacyReportingName() = nestedTest2()
                id.getSource() = Optional[MethodSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest$NestedGroup', methodName = 'nestedTest2', methodParameterTypes = '']]
                id.getTags() = []
            id.getUniqueId() = [engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[nested-class:NestedGroup]/[method:nestedTest()]
                id.getParentId() = Optional[[engine:junit-jupiter]/[class:testauto.testcode.e2e.rebm.RebmCustGroupTest]/[nested-class:NestedGroup]]
                id.getDisplayName() = nestedTest()
                id.getType().name() = TEST
                id.getLegacyReportingName() = nestedTest()
                id.getSource() = Optional[MethodSource [className = 'testauto.testcode.e2e.rebm.RebmCustGroupTest$NestedGroup', methodName = 'nestedTest', methodParameterTypes = '']]
                id.getTags() = []
             */
        }
        private void printTestPlan(TestIdentifier id){
            System.out.println("id.getUniqueId() = " + id.getUniqueId());
            System.out.println("    id.getParentId() = " + id.getParentId());
            System.out.println("    id.getDisplayName() = " + id.getDisplayName());
            System.out.println("    id.getType().name() = " + id.getType().name());
            System.out.println("    id.getLegacyReportingName() = " + id.getLegacyReportingName());
            System.out.println("    id.getSource() = " + id.getSource());
            System.out.println("    id.getTags() = " + id.getTags());
        }
    }

}
