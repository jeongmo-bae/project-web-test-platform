package testauto.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@RequiredArgsConstructor
class TestCatalogServiceImplTest {
    @Autowired
    private TestCatalogService testCatalogService;

    @Test
    void refreshTestCatalog() {
        testCatalogService.refreshTestCatalog();

    }

    @Test
    void discoverAllTests() {
    }

    @Test
    void getClassDetail() {
    }
}