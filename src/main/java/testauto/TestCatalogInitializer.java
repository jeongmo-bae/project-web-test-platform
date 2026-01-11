package testauto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import testauto.service.TestCatalogService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCatalogInitializer implements ApplicationRunner {

    private final TestCatalogService testCatalogService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            testCatalogService.refreshTestCatalog();
        } catch (Exception e) {
            log.warn("Failed to refresh test catalog on startup: {}. " +
                    "Make sure testcode project exists at configured path. " +
                    "You can manually refresh via API.", e.getMessage());
        }
    }
}