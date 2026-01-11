package testauto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import testauto.domain.TestNode;
import testauto.dto.ClassDetailDto;
import testauto.dto.TestMethodDto;
import testauto.repository.TestNodeRepository;
import testauto.runner.TestRunner;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCatalogServiceImpl implements TestCatalogService {

    private final TestNodeRepository repository;
    private final ProcessExecutorService processExecutorService;

    @Value("${testcode.root-package:testauto.testcode}")
    private String testcodeRootPackage;

    @Override
    public void refreshTestCatalog() {
        try {
            // 1. 테스트 코드 컴파일
            processExecutorService.compileTestCode();

            // 2. 별도 JVM에서 테스트 발견
            TestRunner.DiscoverResult result = processExecutorService.discoverTests(testcodeRootPackage);

            if (!result.success()) {
                throw new RuntimeException("Test discovery failed: " + result.error());
            }

            // 3. 결과를 TestNode로 변환하여 DB 저장
            repository.deleteAll();
            List<TestNode> testNodes = result.nodes().stream()
                    .map(dto -> TestNode.builder()
                            .uniqueId(dto.uniqueId())
                            .parentUniqueId(dto.parentUniqueId())
                            .displayName(dto.displayName())
                            .className(dto.className())
                            .type(dto.type())
                            .build())
                    .collect(Collectors.toList());
            repository.saveAll(testNodes);

            log.info("Test catalog refreshed: {} nodes discovered", testNodes.size());

        } catch (Exception e) {
            log.error("Failed to refresh test catalog", e);
            throw new RuntimeException("Failed to refresh test catalog: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TestNode> discoverAllTests() {
        return repository.findAll();
    }

    @Override
    public ClassDetailDto getClassDetail(String className) {
        // DB에서 해당 클래스의 테스트 노드들을 가져와서 트리 구조로 변환
        List<TestNode> allNodes = repository.findAll();

        // 해당 클래스의 루트 노드 찾기
        TestNode classNode = allNodes.stream()
                .filter(node -> className.equals(node.getClassName()) && "CONTAINER".equals(node.getType()))
                .filter(node -> !node.getUniqueId().contains("[nested-class:"))
                .findFirst()
                .orElse(null);

        if (classNode == null) {
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            return ClassDetailDto.builder()
                    .className(simpleClassName)
                    .fullClassName(className)
                    .methods(List.of())
                    .build();
        }

        // uniqueId -> TestNode 맵 생성
        Map<String, TestNode> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(TestNode::getUniqueId, n -> n));

        // parentId -> children 맵 생성
        Map<String, List<TestNode>> childrenMap = allNodes.stream()
                .filter(n -> n.getParentUniqueId() != null)
                .collect(Collectors.groupingBy(TestNode::getParentUniqueId));

        // 클래스 노드의 자식들을 TestMethodDto로 변환
        List<TestMethodDto> methods = buildMethodsHierarchy(classNode.getUniqueId(), childrenMap);

        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

        return ClassDetailDto.builder()
                .className(simpleClassName)
                .fullClassName(className)
                .methods(methods)
                .build();
    }

    private List<TestMethodDto> buildMethodsHierarchy(String parentId, Map<String, List<TestNode>> childrenMap) {
        List<TestNode> children = childrenMap.getOrDefault(parentId, List.of());
        List<TestMethodDto> result = new ArrayList<>();

        for (TestNode child : children) {
            boolean isNestedClass = child.getUniqueId().contains("[nested-class:");
            boolean isTest = "TEST".equals(child.getType());

            if (isTest) {
                // 테스트 메서드
                String methodName = extractMethodNameFromUniqueId(child.getUniqueId());
                result.add(TestMethodDto.builder()
                        .methodName(methodName)
                        .displayName(child.getDisplayName())
                        .uniqueId(child.getUniqueId())
                        .isNestedClass(false)
                        .build());
            } else if (isNestedClass) {
                // Nested 클래스
                List<TestMethodDto> nestedMethods = buildMethodsHierarchy(child.getUniqueId(), childrenMap);
                result.add(TestMethodDto.builder()
                        .displayName(child.getDisplayName())
                        .uniqueId(child.getUniqueId())
                        .isNestedClass(true)
                        .children(nestedMethods)
                        .build());
            } else {
                // 기타 컨테이너 (재귀 탐색)
                result.addAll(buildMethodsHierarchy(child.getUniqueId(), childrenMap));
            }
        }

        return result;
    }

    private String extractMethodNameFromUniqueId(String uniqueId) {
        // uniqueId에서 메서드 이름 추출: ...[method:methodName()]
        int methodStart = uniqueId.lastIndexOf("[method:");
        if (methodStart != -1) {
            int methodEnd = uniqueId.indexOf("(", methodStart);
            if (methodEnd != -1) {
                return uniqueId.substring(methodStart + 8, methodEnd);
            }
            methodEnd = uniqueId.indexOf("]", methodStart);
            if (methodEnd != -1) {
                return uniqueId.substring(methodStart + 8, methodEnd);
            }
        }
        return uniqueId;
    }
}
