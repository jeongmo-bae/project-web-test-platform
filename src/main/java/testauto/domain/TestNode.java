package testauto.domain;

import lombok.*;

@Getter
@Builder
@EqualsAndHashCode( of = {"uniqueId"})
@ToString
public class TestNode {
    private final String uniqueId;
    private final String parentUniqueId;
    private final String displayName;
    private final String className;
    private final String type;
}
