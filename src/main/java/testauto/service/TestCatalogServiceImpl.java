package testauto.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import testauto.domain.TestProgram;
import testauto.repository.TestProgramRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCatalogServiceImpl implements TestCatalogService {
    private final TestProgramRepository repository;

    @Override
    public void refreshTestCatalog() {
        repository.deleteAll();
        List<TestProgram> testProgramList = discoverAllTests();
        repository.saveAll(testProgramList);
    }

    @Override
    public List<TestProgram> discoverAllTests() {
        List<TestProgram> testPrograms = new ArrayList<>();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(
                        DiscoverySelectors.selectPackage("testauto.testcode")
                )
                .build();
        TestPlan testPlan = LauncherFactory.create().discover(request);
        Set<TestIdentifier> roots = testPlan.getRoots();
        for (TestIdentifier id : roots) {
//            discoverChildrenOf(id);

            Set<TestIdentifier> children = testPlan.getChildren(id);

            children.forEach(child -> {
                String uniqueId = child.getUniqueId();
                String parentId = child.getParentId().orElse(null);
                String displayName = child.getDisplayName();
                ClassSource classSource = child.getSource()
                        .filter(ClassSource.class::isInstance)
                        .map(ClassSource.class::cast)
                        .orElse(null);

                TestProgram testProgram = TestProgram.builder()
                        .uniqueId(uniqueId)
                        .parentUniqueId(parentId)
                        .displayName(displayName)
                        .className(classSource != null ? classSource.getClassName() : null)
                        .build();

                testPrograms.add(testProgram);
                System.out.println("=================================================");
                System.out.println("child = " + child);
                System.out.println("child.getType() = " + child.getType());
                System.out.println("child.getSource() = " + child.getSource());
                System.out.println("child.getDisplayName() = " + child.getDisplayName());
                System.out.println("child.isTest() = " + child.isTest());

                System.out.println("testObject.toString() = " + testProgram.toString());
            });
        }
        return testPrograms;
    }

    private List<TestProgram> discoverChildrenOf(TestIdentifier testIdentifier) {
        List<TestProgram> testPrograms = new ArrayList<>();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        DiscoverySelectors.selectClass(testIdentifier.getUniqueId())
                )
                .build();
        TestPlan testPlan = LauncherFactory.create().discover(request);
        Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
        children.forEach(child -> {
            System.out.println("child = " + child);
        });
        return testPrograms;
    }


}
