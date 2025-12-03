package testauto.testcode.e2e.rebm;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}

