package testauto.domain;

import lombok.*;
import org.junit.platform.engine.UniqueId;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode( of = {"uniqueId"})
@ToString
public class TestProgram {
    private final UniqueId uniqueId;
    private UniqueId parentUniqueId;
    private String displayName;
    private String className;
}
