package testauto.dto;

import java.util.ArrayList;
import java.util.List;

public class TestNodeDto {
    private String name;
    private boolean leaf;
    private String className;
    private List<TestNodeDto> children = new ArrayList<>();
}
