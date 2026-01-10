package testauto.testcode.e2e.ebm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

public class EbmNotManagementActivityTest {
    @BeforeAll
    @DisplayName("insert test data & submit job schedule")
    public static void beforeAll() {
        System.out.println("insert test data");
        System.out.println("submit job schedule");
    }

    @Nested
    @DisplayName("관리활동이 아닌, 본부 EBM 선호시간 활용 캠페인 테스트")
    public class EBMNotManagementActivityRecomendedTimeTest {
        @Test
        @DisplayName("수행 고객군 정상 생성 테스트")
        void test1() {
            System.out.println("성공 시에도 로그 출력 테스트");
            Assertions.assertThat(1).isEqualTo(1);
        }
        @Test
        @DisplayName("수행 고객군 제외로직 정상 테스트")
        void test2() {
            Assertions.assertThat(1).isEqualTo(2);
        }
        @Test
        @DisplayName("수행 활동 정상 생성 테스트")
        void test3() {
            Assertions.assertThat(1).isEqualTo(3);
        }

        @Test
        @DisplayName("수행 활동 고객군 정상 생성 테스트")
        void test4() {
            Assertions.assertThat(1).isEqualTo(4);
        }
        @Test
        @DisplayName("수행 활동 정상 종료 및 수행캠페인 정상 종료 테스트")
        void test5() {
            Assertions.assertThat(1).isEqualTo(5);
        }

        @AfterAll
        @DisplayName("Test Data Rollback")
        public static void afterAll() {
//            Assertions.assertThat(1).isEqualTo(6);
            System.out.println("AftertAll - Test Data Rollback");
        }
    }
    @Nested
    @DisplayName("관리활동이 아닌, 본부 EBM 선호시간 미활용 캠페인 테스트")
    public class EBMNotManagementActivityUserDefinedTimeTest {
        @Test
        @DisplayName("수행 고객군 정상 생성 테스트")
        void test1() throws InterruptedException {
            Thread.sleep(100000);
            Assertions.assertThat(1).isEqualTo(1);
        }
        @Test
        @DisplayName("수행 고객군 제외로직 정상 테스트")
        void test2() {
            Assertions.assertThat(1).isEqualTo(2);
        }
        @Test
        @DisplayName("수행 활동 정상 생성 테스트")
        void test3() {
            Assertions.assertThat(1).isEqualTo(3);
        }

        @Test
        @DisplayName("수행 활동 고객군 정상 생성 테스트")
        void test4() {
            Assertions.assertThat(1).isEqualTo(4);
        }
        @Test
        @DisplayName("수행 활동 정상 종료 및 수행캠페인 정상 종료 테스트")
        void test5() {
            Assertions.assertThat(1).isEqualTo(5);
        }

        @AfterAll
        @DisplayName("Test Data Rollback")
        public static void afterAll() {
//            Assertions.assertThat(1).isEqualTo(6);
            System.out.println("AftertAll - Test Data Rollback");
        }
    }
}
