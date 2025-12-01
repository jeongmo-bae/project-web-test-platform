package testauto.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode( of = {"uniqueId"})
public class TestObject {
    private String uniqueId;
    private String displayName;
    private String className;
}
