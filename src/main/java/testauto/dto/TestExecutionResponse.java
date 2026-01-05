package testauto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TestExecutionResponse {
    private String executionId;
    private String status;
    private String message;
}
