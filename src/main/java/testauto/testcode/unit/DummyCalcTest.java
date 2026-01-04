
package testauto.testcode.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DummyCalcTest {
    @Test
    @DisplayName("성공 테스트")
    void add_test() {
        int a = 1 + 2;
        assertEquals(3, a);
    }

    @Test
    @DisplayName("실패테스트")
    void fail_test() {
        int a = 1 + 2;
        assertEquals(2, a); // 일부러 실패시키기
    }
}