package testauto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import testauto.domain.TestNode;
import testauto.dto.TreeNodeDto;
import testauto.repository.TestNodeDbRepository;
import testauto.repository.TestNodeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramTreeServiceImpl implements ProgramTreeService {

    private static final String ENGINE_ROOT_ID = "[engine:junit-jupiter]";
    private static final String BASE_PACKAGE = "testauto.testcode";
    private static final String BASE_PACKAGE_LEAF = "testcode"; // 화면에 찍힐 루트 라벨

    private final TestNodeRepository testNodeRepository;

    @Override
    public TreeNodeDto buildTree() {

        // 1) 엔진 루트 바로 아래 자식들 중에서 클래스(CONTAINER + className 존재)만 뽑기
        List<TestNode> directChildren = testNodeRepository.findByParentId(ENGINE_ROOT_ID);

        List<TestNode> classNodes = new ArrayList<>();
        for (TestNode node : directChildren) {
            if (!"CONTAINER".equals(node.getType())) continue;
            if (node.getClassName() == null) continue;
            if (!node.getClassName().startsWith(BASE_PACKAGE)) continue;

            classNodes.add(node);
        }

        // 2) 트리 루트 노드 생성 (화면 왼쪽 상단에 표시할 패키지명)
        TreeNodeDto root = TreeNodeDto.builder()
                .name(BASE_PACKAGE_LEAF)   // "testcode"
                .type("PACKAGE")
                .build();

        // 3) 각 클래스 노드를 패키지 구조에 따라 트리에 추가
        for (TestNode classNode : classNodes) {
            addClassToTree(root, classNode);
        }

        return root;
    }

    private void addClassToTree(TreeNodeDto root, TestNode classNode) {
        String className = classNode.getClassName(); // 예: testauto.testcode.e2e.ebm.EventBased...

        String[] tokens = className.split("\\.");

        // "testcode" 가 어디 있는지 찾기
        int startIndex = -1;
        for (int i = 0; i < tokens.length; i++) {
            if (BASE_PACKAGE_LEAF.equals(tokens[i])) {
                startIndex = i;
                break;
            }
        }

        // 이상한 경우면 무시
        if (startIndex == -1 || startIndex >= tokens.length - 1) {
            return;
        }

        TreeNodeDto current = root;

        // 패키지 조각들 (e2e / ebm / ... ) 생성
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
                        .type("PACKAGE")
                        .build();
                current.getChildren().add(next);
            }

            current = next;
        }

        // 마지막 토큰은 클래스 이름
        String simpleClassName = tokens[tokens.length - 1];

        TreeNodeDto classDto = TreeNodeDto.builder()
                .name(simpleClassName)
                .type("CLASS")
                .uniqueId(classNode.getUniqueId())
                .parentUniqueId(classNode.getParentUniqueId())
                .className(classNode.getClassName())
                .build();

        current.getChildren().add(classDto);
    }
}
