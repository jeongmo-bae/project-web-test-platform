package testauto.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class TreeNodeDto {

    public enum NodeType {
        PACKAGE,
        CLASS
    }
    // 화면에 찍힐 이름
    private String name;

    // 클래스 노드일 때만 의미 있는 값들
    private String uniqueId;
    private String parentUniqueId;
    private String className;
    private String type;
    private List<TreeNodeDto> children ;

    @Builder
    private TreeNodeDto(
            String name,
            String uniqueId,
            String parentUniqueId,
            String className,
            NodeType type,
            List<TreeNodeDto> children
    ) {
        this.name = name;
        this.uniqueId = uniqueId;
        this.parentUniqueId = parentUniqueId;
        this.className = className;
        this.type = (type != null) ? type.name() : null; // 여기서 enum→String 변환
        this.children = (children != null) ? children : new ArrayList<>();
    }

}
