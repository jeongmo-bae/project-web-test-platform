package testauto.repository;

import testauto.domain.TestResult;
import testauto.domain.TestStatus;
import testauto.domain.TestSummary;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TestResultMemoryRepository {

    // id -> TestResult
    private final Map<String, TestResult> nodeMap = new ConcurrentHashMap<>();

    // 루트 노드들 (보통 엔진/테스트 클래스 단위)
    private final List<TestResult> roots = Collections.synchronizedList(new ArrayList<>());

    // 시작 시간 측정용
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();

    public void clear() {
        nodeMap.clear();
        roots.clear();
        startTimeMap.clear();
    }

    public void markStarted(String id, String displayName, String parentId) {
        TestResult node = new TestResult(id, displayName);
        nodeMap.put(id, node);

        if (parentId != null) {
            TestResult parent = nodeMap.get(parentId);
            if (parent != null) {
                parent.addChild(node);
            } else {
                roots.add(node);
            }
        } else {
            roots.add(node);
        }

        startTimeMap.put(id, System.currentTimeMillis());
    }

    public void markFinished(String id,
                             TestStatus status,
                             String errorMessage,
                             String stackTrace) {

        TestResult node = nodeMap.get(id);
        if (node == null) {
            return;
        }

        node.setStatus(status);

        Long startTime = startTimeMap.get(id);
        if (startTime != null) {
            node.setDurationMillis(System.currentTimeMillis() - startTime);
        }

        node.setErrorMessage(errorMessage);
        node.setStackTrace(stackTrace);
    }

    public List<TestResult> findRootResults() {
        return new ArrayList<>(roots);
    }

    public TestSummary buildSummary() {
        TestSummary summary = new TestSummary();
        for (TestResult root : roots) {
            accumulate(summary, root);
        }
        return summary;
    }

    private void accumulate(TestSummary summary, TestResult node) {
        // 자식이 없는 노드(실제 테스트 메서드)만 카운트
        // JUnit Jupiter 엔진, 클래스, Nested 클래스는 제외
        if (node.getChildren().isEmpty()) {
            summary.incTotal();
            summary.addDuration(node.getDurationMillis());

            if (node.getStatus() == TestStatus.SUCCESS) {
                summary.incSuccess();
            } else if (node.getStatus() == TestStatus.FAILED) {
                summary.incFailed();
            } else if (node.getStatus() == TestStatus.SKIPPED) {
                summary.incSkipped();
            }
        }

        // 자식 노드들에 대해 재귀적으로 처리
        for (TestResult child : node.getChildren()) {
            accumulate(summary, child);
        }
    }
}