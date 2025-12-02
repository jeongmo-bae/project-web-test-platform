package testauto.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import testauto.domain.TestNode;
import testauto.repository.TestNodeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCatalogServiceImpl implements TestCatalogService {
    private final TestNodeRepository repository;
    private final String testCodePackage = "testauto.testcode";

    @Override
    public void refreshTestCatalog() {
        repository.deleteAll();
        List<TestNode> testNodeList = discoverAllTests();
        repository.saveAll(testNodeList);
    }

    @Override
    public List<TestNode> discoverAllTests() {
        List<TestNode> testNodes = new ArrayList<>();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(
                        DiscoverySelectors.selectPackage(testCodePackage)
                )
                .build();
        TestPlan testPlan = LauncherFactory.create().discover(request);
        Set<TestIdentifier> roots = testPlan.getRoots();
        for (TestIdentifier root : roots) {
            collectNodes(testPlan, root, testNodes);
        }
        return testNodes;
    }

    private void collectNodes(TestPlan testPlan, TestIdentifier testIdentifier, List<TestNode> testNodes) {
        String uniqueId = testIdentifier.getUniqueId();
        String parentId = testIdentifier.getParentId().orElse(null);
        String displayName = testIdentifier.getDisplayName();
        ClassSource classSource = getNodeClassSource(testIdentifier);
        String nodeType = getNodeType(testIdentifier);

        TestNode testNode = TestNode.builder()
                .uniqueId(uniqueId)
                .parentUniqueId(parentId)
                .displayName(displayName)
                .className(classSource != null ? classSource.getClassName() : null)
                .type(nodeType)
                .build();
        testNodes.add(testNode);

        Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
        for (TestIdentifier child : children){
            collectNodes(testPlan, child, testNodes);
        }
    }

    private ClassSource getNodeClassSource(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                .filter(ClassSource.class::isInstance)
                .map(ClassSource.class::cast)
                .orElse(null);
    }

    private String getNodeType(TestIdentifier testIdentifier){
        return switch (testIdentifier.getType()){
            case CONTAINER -> "CONTAINER";
            case TEST -> "TEST";
            default -> "UNKNOWN";
        };
    }





}
