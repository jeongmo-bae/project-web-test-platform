package testauto.dto;

import java.util.ArrayList;
import java.util.List;

public class TestProgramNode {
    private String name;
    private boolean leaf;
    private String className;
    private List<TestProgramNode> children = new ArrayList<>();
}
