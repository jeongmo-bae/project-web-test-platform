package testauto.testcode.e2e.ebm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EventBasedCampaignNotManagementActivityTest {
    @Nested
    @DisplayName("관리활동이 아닌, 본부 EBM 선호시간 활용 캠페인 테스트")
    public class EBMNotManagementActivityRecomendedTimeTest {
        @Test
        @DisplayName("수행 고객군 정상 생성 테스트")
        void test1() {
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

        @Test
        @DisplayName("Test Data Rollback")
        void test6() {
            Assertions.assertThat(1).isEqualTo(6);
        }
    }
    @Nested
    @DisplayName("관리활동이 아닌, 본부 EBM 선호시간 미활용 캠페인 테스트")
    public class EBMNotManagementActivityUserDefinedTimeTest {
        @Test
        @DisplayName("수행 고객군 정상 생성 테스트")
        void test1() {
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

        @Test
        @DisplayName("Test Data Rollback")
        void test6() {
            Assertions.assertThat(1).isEqualTo(6);
        }
    }
}
