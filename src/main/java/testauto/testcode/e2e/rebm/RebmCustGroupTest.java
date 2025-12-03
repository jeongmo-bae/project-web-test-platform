package testauto.testcode.e2e.rebm;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("이건 Rebm 테스트 클래스로\n" +
        " 고객군 검증을 할거야\n" +
        " 마ㅓㄴ오마너옴")
public class RebmCustGroupTest {
    @Test
    @DisplayName("플랫폼 개발용 테스트 메서드 입니다~ 성공테스트~~")
    void successTest() {
        Assertions.assertThat(1).isEqualTo(1);
    }

    @Test
    @DisplayName("플랫폼 개발용 테스트 메서드 입니다! 실패 테스트~~")
    void failTest(){
        Assertions.assertThat(1).isEqualTo(2);
    }

    @Test
    @DisplayName("플랫폼 개발용이요~ 예외정상 발생 테스트 예시요~~")
    void exceptionTest(){
        assertThrows(IllegalStateException.class,()->{
            new TestClassForExceptionTest();
        });
    }
    @RequiredArgsConstructor
    private final class TestClassForExceptionTest{
        @NonNull
        private String testParam;

        TestClassForExceptionTest(){
            throw new IllegalStateException("비상일리걸스테이트익셉션발생");
        }
    }

    @Nested
    class NestedGroup {
        @Test
        void nestedTest() {
            System.out.println("nestedTest run");
        }

        @Test
        @DisplayName("네스티드 내에 정의된 메서드 2~")
        void nestedTest2() {
            System.out.println("nestedTest2 run");
        }
    }
}

