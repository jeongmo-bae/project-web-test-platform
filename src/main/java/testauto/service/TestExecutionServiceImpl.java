package testauto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import testauto.repository.TestExecutionRepository;
import testauto.domain.TestExecution;
import testauto.domain.TestResult;
import testauto.domain.TestResultRecord;
import testauto.domain.TestSummary;
import testauto.util.junit.WebTestListener;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestExecutionServiceImpl implements TestExecutionService {

    private final TestExecutionRepository executionRepository;

    /**
     * 테스트 실행 요청 (비동기) - executionId 즉시 반환
     */
    @Override
    public String submitTests(List<String> classNames, String requesterIp) {
        String executionId = UUID.randomUUID().toString();
        log.info("Submitting test execution {} for {} classes from IP: {}",
                executionId, classNames.size(), requesterIp);

        // 실행 시작 기록 (RUNNING 상태)
        TestExecution execution = TestExecution.builder()
                .executionId(executionId)
                .startedAt(LocalDateTime.now())
                .requesterIp(requesterIp)
                .classNames(String.join(",", classNames))
                .status("RUNNING")
                .build();

        try {
            executionRepository.saveExecution(execution);
        } catch (Exception e) {
            log.warn("Failed to save execution start to DB: {}", e.getMessage());
        }

        // 비동기로 테스트 실행
        executeTestsAsync(executionId, classNames);

        return executionId;
    }

    /**
     * 테스트 실제 실행 (비동기)
     */
    @Async
    public void executeTestsAsync(String executionId, List<String> classNames) {
        log.info("Starting async test execution {} for {} classes", executionId, classNames.size());
        log.debug("Classes to execute: {}", classNames);

        // 각 실행마다 새로운 리스너 생성
        WebTestListener listener = new WebTestListener();

        try {
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(listener);

            List<DiscoverySelector> selectors = classNames.stream()
                    .<DiscoverySelector>map(className -> {
                        try {
                            return DiscoverySelectors.selectClass(className);
                        } catch (Exception e) {
                            log.warn("Failed to select class: {}", className, e);
                            throw new RuntimeException("Invalid class name: " + className, e);
                        }
                    })
                    .toList();

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectors)
                    .build();

            launcher.execute(request);
            log.info("Test execution {} completed for {} classes", executionId, classNames.size());

            // 실행 완료 후 DB에 결과 저장
            saveResultsToDb(executionId, listener);

        } catch (Exception e) {
            log.error("Failed to execute tests for execution {}: {}", executionId, e.getMessage(), e);

            // 실패 상태로 업데이트
            try {
                TestExecution failedExecution = TestExecution.builder()
                        .executionId(executionId)
                        .finishedAt(LocalDateTime.now())
                        .status("FAILED")
                        .build();
                executionRepository.updateExecution(failedExecution);
            } catch (Exception updateEx) {
                log.error("Failed to update execution status to FAILED: {}", updateEx.getMessage());
            }
        }
    }

    /**
     * 테스트 결과를 DB에 저장
     */
    private void saveResultsToDb(String executionId, WebTestListener listener) {
        try {
            TestSummary summary = listener.buildSummary();
            List<TestResult> results = listener.findRootResults();

            // 실행 정보 업데이트
            TestExecution execution = TestExecution.builder()
                    .executionId(executionId)
                    .finishedAt(LocalDateTime.now())
                    .totalTests(summary.getTotal())
                    .successCount(summary.getSuccess())
                    .failedCount(summary.getFailed())
                    .skippedCount(summary.getSkipped())
                    .totalDurationMillis(summary.getTotalDurationMillis())
                    .status("COMPLETED")
                    .build();

            executionRepository.updateExecution(execution);

            // 결과 상세 저장
            List<TestResultRecord> records = new ArrayList<>();
            for (TestResult result : results) {
                collectResultRecords(executionId, result, null, records);
            }
            executionRepository.saveAllResults(records);

            log.info("Saved {} test results to DB for execution {}", records.size(), executionId);
        } catch (Exception e) {
            log.error("Failed to save test results to DB: {}", e.getMessage(), e);
        }
    }

    /**
     * TestResult 트리를 TestResultRecord 리스트로 변환
     */
    private void collectResultRecords(String executionId, TestResult result,
                                       String parentId, List<TestResultRecord> records) {
        TestResultRecord record = TestResultRecord.builder()
                .executionId(executionId)
                .testId(result.getId())
                .parentTestId(parentId)
                .displayName(result.getDisplayName())
                .status(result.getStatus())
                .durationMillis(result.getDurationMillis())
                .errorMessage(result.getErrorMessage())
                .stackTrace(result.getStackTrace())
                .stdout(result.getStdout())
                .build();
        records.add(record);

        for (TestResult child : result.getChildren()) {
            collectResultRecords(executionId, child, result.getId(), records);
        }
    }

    /**
     * 최근 실행 이력 조회
     */
    @Override
    public List<TestExecution> getRecentExecutions(int limit) {
        return executionRepository.findRecentExecutions(limit);
    }

    /**
     * 특정 실행 조회
     */
    @Override
    public Optional<TestExecution> getExecution(String executionId) {
        return executionRepository.findExecutionById(executionId);
    }

    /**
     * 특정 실행의 상세 결과 조회
     */
    @Override
    public List<TestResultRecord> getExecutionResults(String executionId) {
        return executionRepository.findResultsByExecutionId(executionId);
    }

    /**
     * 특정 실행의 결과를 트리 형태로 조회
     */
    @Override
    public List<TestResult> getExecutionResultTree(String executionId) {
        List<TestResultRecord> records = executionRepository.findResultsByExecutionId(executionId);
        return buildResultTree(records);
    }

    /**
     * TestResultRecord 리스트를 TestResult 트리로 변환
     */
    private List<TestResult> buildResultTree(List<TestResultRecord> records) {
        Map<String, TestResult> resultMap = new HashMap<>();
        List<TestResult> roots = new ArrayList<>();

        // 모든 레코드를 TestResult로 변환
        for (TestResultRecord record : records) {
            TestResult result = new TestResult(record.getTestId(), record.getDisplayName());
            result.setStatus(record.getStatus());
            result.setDurationMillis(record.getDurationMillis());
            result.setErrorMessage(record.getErrorMessage());
            result.setStackTrace(record.getStackTrace());
            result.setStdout(record.getStdout());
            resultMap.put(record.getTestId(), result);
        }

        // 부모-자식 관계 설정
        for (TestResultRecord record : records) {
            TestResult result = resultMap.get(record.getTestId());
            if (record.getParentTestId() == null) {
                roots.add(result);
            } else {
                TestResult parent = resultMap.get(record.getParentTestId());
                if (parent != null) {
                    parent.addChild(result);
                } else {
                    roots.add(result);
                }
            }
        }

        return roots;
    }
}
