package testauto.domain;

import lombok.Getter;

@Getter
public class TestSummary {
    private int total;
    private int success;
    private int failed;
    private int skipped;
    private long totalDurationMillis;

    public void incTotal() {
        this.total++;
    }

    public void incSuccess() {
        this.success++;
    }

    public void incFailed() {
        this.failed++;
    }

    public void incSkipped() {
        this.skipped++;
    }

    public void addDuration(long durationMillis) {
        this.totalDurationMillis += durationMillis;
    }
}
