package testauto.testcode.unit;

import org.junit.jupiter.api.*;

public class SampleTest {

    @BeforeAll
    static void beforeAll() {
        System.out.println("[beforeAll] SampleTest");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("[beforeEach]");
    }

    @Test
    @DisplayName("성공 테스트 입니다")
    void successTest() {
        System.out.println("successTest run");
        Assertions.assertTrue(true);
    }

    @Test
    @DisplayName("실패 테스트 입니다")
    void failedTest() {
        System.out.println("failedTest run");
        Assertions.fail("의도적으로 실패");
    }

    @Nested
    @DisplayName("Nested Group 테스트 입니다")
    public class NestedGroup {

        @Test
        @DisplayName("Nested1 테스트 입니다")
        void nestedTest() {
            System.out.println("nestedTest run");
            org.assertj.core.api.Assertions.assertThat(1).isEqualTo(1);
        }

        @Test
        @DisplayName("Nested2 테스트 입니다")
        void nestedTest2() {
            System.out.println("nestedTest2 run");
            org.assertj.core.api.Assertions.assertThat(1).isEqualTo(2);
        }
    }
}
