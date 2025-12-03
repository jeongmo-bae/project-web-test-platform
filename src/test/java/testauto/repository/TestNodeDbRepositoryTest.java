package testauto.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import testauto.domain.TestNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TestNodeDbRepositoryTest {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    TestNodeDbRepositoryTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void findByUniqueId() {
        //given
        TestNodeRepository repository = new TestNodeDbRepository(jdbcTemplate);
        //when
        TestNode testNode = repository.findByUniqueId("[engine:junit-jupiter]/[class:testauto.testcode.unit.DummyCalcTest]/[method:fail_test()]").orElse(null);
        //then
        System.out.println(testNode.toString());
        Assertions.assertThat(testNode).isNotNull();
        Assertions.assertThat(testNode.getType()).isEqualTo("TEST");
        Assertions.assertThat(testNode.getDisplayName()).isEqualTo("실패테스트");
        Assertions.assertThat(testNode).isEqualTo(
                TestNode.builder().uniqueId("[engine:junit-jupiter]/[class:testauto.testcode.unit.DummyCalcTest]/[method:fail_test()]").build()
        );

    }

    @Test
    void findByParentId() {
        //given
        TestNodeRepository repository = new TestNodeDbRepository(jdbcTemplate);
        //when
        List<TestNode> testNodes = repository.findByParentId("[engine:junit-jupiter]");
        //then
        testNodes.forEach(System.out::println);
        Assertions.assertThat(testNodes).isNotEmpty();
        Assertions.assertThat(testNodes.size()).isEqualTo(5);
    }
}