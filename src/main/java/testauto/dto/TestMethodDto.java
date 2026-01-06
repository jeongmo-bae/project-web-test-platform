package testauto.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TestMethodDto {
    private String methodName;
    private String displayName;
    private String uniqueId;
    private boolean isNestedClass;
    @Builder.Default
    private List<TestMethodDto> children = List.of();
}
