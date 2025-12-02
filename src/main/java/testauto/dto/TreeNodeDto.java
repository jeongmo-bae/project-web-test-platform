package testauto.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class TreeNodeDto {

    // 화면에 찍힐 이름 (패키지 조각 이름 or 클래스 simple name)
    private String name;

    // 클래스 노드일 때만 의미 있는 값들
    private String uniqueId;
    private String parentUniqueId;
    private String className;

    // "PACKAGE" 또는 "CLASS"
    private String type;

    @Builder.Default
    private List<TreeNodeDto> children = new ArrayList<>();
}
