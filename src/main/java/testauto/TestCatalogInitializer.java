package testauto;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import testauto.service.TestCatalogService;

@Component
@RequiredArgsConstructor
public class TestCatalogInitializer implements ApplicationRunner {

    private final TestCatalogService testCatalogService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        testCatalogService.refreshTestCatalog();
    }
}