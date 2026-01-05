package testauto.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;
    private final Map<String, String> details;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
}
