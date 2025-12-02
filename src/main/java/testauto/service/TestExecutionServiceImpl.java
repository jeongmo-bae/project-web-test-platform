package testauto.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class TestExecutionServiceImpl {

    private final WebTestListener webTestListener;
    private final TestResultMemoryRepository repository;

    public void runTests(String className) {
        repository.clear();

        Launcher launcher = LauncherFactory.create();

        launcher.registerTestExecutionListeners(webTestListener);

        DiscoverySelector selector = DiscoverySelectors.selectClass(className);

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(List.of(selector))
                .build();

        launcher.execute(request);
    }

    public TestSummary getSummary() {
        return repository.buildSummary();
    }

    public List<TestResult> getTestTree() {
        return repository.findRootResults();
    }
}