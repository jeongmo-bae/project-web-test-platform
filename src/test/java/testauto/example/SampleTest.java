package testauto.example;

import org.junit.jupiter.api.*;

class SampleTest {

    @BeforeAll
    static void beforeAll() {
        System.out.println("[beforeAll] SampleTest");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("[beforeEach]");
    }

    @Test
    void successTest() {
        System.out.println("successTest run");
        Assertions.assertTrue(true);
    }

    @Test
    void failedTest() {
        System.out.println("failedTest run");
        Assertions.fail("의도적으로 실패");
    }

    @Nested
    class NestedGroup {

        @Test
        void nestedTest() {
            System.out.println("nestedTest run");
        }
    }
}