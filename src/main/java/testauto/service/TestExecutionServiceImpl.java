package testauto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import testauto.repository.TestResultMemoryRepository;
import testauto.domain.TestResult;
import testauto.domain.TestSummary;
import testauto.util.junit.WebTestListener;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestExecutionServiceImpl implements TestExecutionService {

    private final WebTestListener webTestListener;
    private final TestResultMemoryRepository repository;

    /**
     * 단일 테스트 클래스 실행
     */
    public synchronized void runTests(String className) {
        log.info("Starting test execution for class: {}", className);
        repository.clear();

        try {
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(webTestListener);

            DiscoverySelector selector = DiscoverySelectors.selectClass(className);

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(List.of(selector))
                    .build();

            launcher.execute(request);
            log.info("Test execution completed for class: {}", className);
        } catch (Exception e) {
            log.error("Failed to execute tests for class: {}", className, e);
            throw new RuntimeException("Test execution failed for class: " + className, e);
        }
    }

    /**
     * 여러 테스트 클래스 실행
     */
    public synchronized void runTests(List<String> classNames) {
        log.info("Starting test execution for {} classes", classNames.size());
        log.debug("Classes to execute: {}", classNames);

        repository.clear();

        try {
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(webTestListener);

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
            log.info("Test execution completed for {} classes", classNames.size());
        } catch (Exception e) {
            log.error("Failed to execute tests for classes: {}", classNames, e);
            throw new RuntimeException("Test execution failed", e);
        }
    }

    /**
     * 테스트 실행 요약 정보 조회
     */
    public TestSummary getSummary() {
        return repository.buildSummary();
    }

    /**
     * 테스트 결과 트리 조회
     */
    public List<TestResult> getTestTree() {
        return repository.findRootResults();
    }
}