package testauto.service;

import lombok.RequiredArgsConstructor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import testauto.domain.TestProgram;
import testauto.repository.TestProgramMemoryRepository;
import testauto.repository.TestProgramRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestDiscoveryServiceImpl implements TestDiscoveryService{
    @Autowired
    private final TestProgramRepository repository;

    public static void main(String[] args) {
        (new TestDiscoveryServiceImpl(new TestProgramMemoryRepository())).discoverAllTests();
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
            Set<TestIdentifier> children = testPlan.getChildren(id);
            children.forEach(child -> {
                TestProgram testProgram = new TestProgram(child.getUniqueIdObject());
                testProgram.setParentUniqueId(child.getParentIdObject().orElse(null));
                testProgram.setDisplayName(child.getDisplayName());
                ClassSource testSource = (ClassSource) child.getSource().orElse(null);
                testProgram.setClassName(testSource.getClassName());
                
                System.out.println("=================================================");
                System.out.println("child = " + child);
                System.out.println("child.getType() = " + child.getType());
                System.out.println("child.getSource() = " + child.getSource());
                System.out.println("child.getDisplayName() = " + child.getDisplayName());
                System.out.println("child.isTest() = " + child.isTest());

                System.out.println("testObject.toString() = " + testProgram.toString());
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
