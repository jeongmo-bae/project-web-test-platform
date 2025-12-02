package testauto.repository;

import testauto.domain.TestNode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestNodeRepository {
    void save(TestNode testNode);
    void saveAll(Collection<TestNode> testNodes);
    Optional<TestNode> findByUniqueId(String uniqueId);
    List<TestNode> findAll();
    List<TestNode> findByParentId(String parentUniqueId);
    void deleteAll();
}
