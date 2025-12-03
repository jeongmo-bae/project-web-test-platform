package testauto;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import testauto.domain.TestNode;
import testauto.repository.TestNodeRepository;
import testauto.service.TestCatalogService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@RequiredArgsConstructor
class TestAutoApplicationTest {
    private final TestCatalogService testCatalogService;
    private final TestNodeRepository repository;

    @Test
    @DisplayName("어플리케이션 실행 시, 테스트플랜 카탈로그 초기화 되는지 확인")
    void testCatalogRefresh() {
        TestNode testNode = repository.findByUniqueId("[engine:junit-jupiter]").orElse(null);

    }
}