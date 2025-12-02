package testauto.service;

import org.junit.jupiter.api.Test;
import testauto.domain.TestNode;
import testauto.repository.TestNodeMemoryRepository;
import testauto.repository.TestNodeRepository;

import java.util.List;

class TestCatalogServiceImplTest {

    @Test
    void refreshTestCatalog() {
        //given
        TestNodeRepository testNodeRepository = new TestNodeMemoryRepository();
        TestCatalogServiceImpl testCatalogService = new TestCatalogServiceImpl(testNodeRepository);
        testCatalogService.refreshTestCatalog();

        //when
        //then
        List<TestNode> testNodes = testNodeRepository.findAll();
        testNodes.forEach(System.out::println);

    }

    @Test
    void discoverAllTests() {
        TestCatalogServiceImpl testCatalogService = new TestCatalogServiceImpl(new TestNodeMemoryRepository());
        List<TestNode> testNodes = testCatalogService.discoverAllTests();
        testNodes.forEach(System.out::println);
    }
}