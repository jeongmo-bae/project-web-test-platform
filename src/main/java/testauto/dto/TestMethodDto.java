package testauto.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestMethodDto {
    private String methodName;
    private String displayName;
    private String uniqueId;
}
