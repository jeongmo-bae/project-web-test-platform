package testauto.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import testauto.domain.TestNode;
import testauto.dto.ClassDetailDto;
import testauto.dto.TestMethodDto;
import testauto.repository.TestNodeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCatalogServiceImpl implements TestCatalogService {
    private final TestNodeRepository repository;
    private final String TESTCODE_ROOT_PACKAGE = "testauto.testcode";

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
                        DiscoverySelectors.selectPackage(TESTCODE_ROOT_PACKAGE)
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

    @Override
    public ClassDetailDto getClassDetail(String className) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectClass(className))
                .build();

        TestPlan testPlan = LauncherFactory.create().discover(request);

        List<TestMethodDto> methods = new ArrayList<>();
        Set<TestIdentifier> roots = testPlan.getRoots();

        for (TestIdentifier root : roots) {
            collectMethods(testPlan, root, methods);
        }

        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

        return ClassDetailDto.builder()
                .className(simpleClassName)
                .fullClassName(className)
                .methods(methods)
                .build();
    }

    private void collectMethods(TestPlan testPlan, TestIdentifier identifier, List<TestMethodDto> methods) {
        if (identifier.isTest()) {
            String methodName = extractMethodName(identifier);
            methods.add(TestMethodDto.builder()
                    .methodName(methodName)
                    .displayName(identifier.getDisplayName())
                    .uniqueId(identifier.getUniqueId())
                    .build());
        }

        Set<TestIdentifier> children = testPlan.getChildren(identifier);
        for (TestIdentifier child : children) {
            collectMethods(testPlan, child, methods);
        }
    }

    /**
     * TestIdentifier에서 실제 메서드 이름을 추출합니다.
     * MethodSource를 사용하여 Java 메서드 이름을 가져옵니다.
     */
    private String extractMethodName(TestIdentifier identifier) {
        return identifier.getSource()
                .filter(MethodSource.class::isInstance)
                .map(MethodSource.class::cast)
                .map(MethodSource::getMethodName)
                .orElse(identifier.getDisplayName()); // fallback to displayName
    }
}
