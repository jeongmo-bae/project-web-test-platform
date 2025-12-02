package testauto.domain;

import lombok.*;
import org.junit.platform.engine.UniqueId;

@Getter
@Builder
@EqualsAndHashCode( of = {"uniqueId"})
@ToString
public class TestProgram {
    private final String uniqueId;
    private final String parentUniqueId;
    private final String displayName;
    private final String className;
    private final String type;
}
