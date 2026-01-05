package testauto.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestExecutionRequest {
    @NotNull(message = "Class names cannot be null")
    @NotEmpty(message = "At least one class name is required")
    private List<String> classNames;
}
