package testauto.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter @Setter
@EqualsAndHashCode(of = {"id","displayName"})
public class TestResult {
    private final String id;              // JUnit UniqueId
    private final String displayName;     // 클래스/메서드 이름
    private TestStatus status;
    private long durationMillis;
    private String errorMessage;
    private String stackTrace;
    private String stdout;                // Standard output captured during test execution
    private List<TestResult> children = new CopyOnWriteArrayList<>();

    public TestResult(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.status = TestStatus.RUNNING;
    }

    public void addChild(TestResult child) {
        this.children.add(child);
    }
}