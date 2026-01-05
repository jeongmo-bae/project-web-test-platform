package testauto.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClassDetailDto {
    private String className;
    private String fullClassName;
    private List<TestMethodDto> methods;
}
