package testauto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import testauto.domain.TestNode;
import testauto.dto.TreeNodeDto;
import testauto.repository.TestNodeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramTreeServiceImpl implements ProgramTreeService {

    private final String ENGINE_ROOT_ID = "[engine:junit-jupiter]";
    private final String BASE_PACKAGE = "testauto.testcode";
    private final String BASE_PACKAGE_LEAF = "testcode";

    private final TestNodeRepository testNodeRepository;

    @Override
    public TreeNodeDto buildTree() {
        List<TestNode> directChildren = testNodeRepository.findByParentId(ENGINE_ROOT_ID);

        List<TestNode> classNodes = new ArrayList<>();
        for (TestNode node : directChildren) {
            if (!node.getType().equals("CONTAINER")) continue;
            if (node.getClassName() == null) continue;
            if (!node.getClassName().startsWith(BASE_PACKAGE)) continue;

            classNodes.add(node);
        }

        TreeNodeDto root = TreeNodeDto.builder()
                .name(BASE_PACKAGE_LEAF)
                .type(TreeNodeDto.NodeType.PACKAGE)
                .build();

        for (TestNode classNode : classNodes) {
            addClassToTree(root, classNode);
        }
        return root;
    }

    private void addClassToTree(TreeNodeDto root, TestNode classNode) {
        String className = classNode.getClassName();

        String[] tokens = className.split("\\.");

        int startIndex = -1;
        for (int i = 0; i < tokens.length; i++) {
            if (BASE_PACKAGE_LEAF.equals(tokens[i])) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == -1 || startIndex >= tokens.length - 1) {
            return;
        }

        TreeNodeDto current = root;

        for (int i = startIndex + 1; i < tokens.length - 1; i++) {
            String segment = tokens[i];

            TreeNodeDto next = null;
            for (TreeNodeDto child : current.getChildren()) {
                if ("PACKAGE".equals(child.getType()) && segment.equals(child.getName())) {
                    next = child;
                    break;
                }
            }

            if (next == null) {
                next = TreeNodeDto.builder()
                        .name(segment)
                        .type(TreeNodeDto.NodeType.PACKAGE)
                        .build();
                current.getChildren().add(next);
            }

            current = next;
        }

        String simpleClassName = tokens[tokens.length - 1];

        TreeNodeDto classDto = TreeNodeDto.builder()
                .name(simpleClassName)
                .type(TreeNodeDto.NodeType.CLASS)
                .uniqueId(classNode.getUniqueId())
                .parentUniqueId(classNode.getParentUniqueId())
                .className(classNode.getClassName())
                .build();

        current.getChildren().add(classDto);
    }
}
