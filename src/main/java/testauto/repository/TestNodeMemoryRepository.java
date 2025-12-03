package testauto.repository;

import lombok.extern.slf4j.Slf4j;
import testauto.domain.TestNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
//@Repository
public class TestNodeMemoryRepository implements TestNodeRepository {
    private final ConcurrentMap<String, TestNode> store = new ConcurrentHashMap<>();

    @Override
    public void save(TestNode testNode) {
        store.put(testNode.getUniqueId(), testNode);
        log.debug("Saved TestProgram : " + testNode);
    }

    @Override
    public void saveAll(Collection<TestNode> testNodes) {
        for (TestNode testNode : testNodes) {
            save(testNode);
        }
    }

    @Override
    public Optional<TestNode> findByUniqueId(String uniqueId) {
        return Optional.ofNullable(store.get(uniqueId));
    }

    @Override
    public List<TestNode> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<TestNode> findByParentId(String parentUniqueId) {
        List<TestNode> result = new ArrayList<>();
        for (TestNode value : store.values()) {
            if (parentUniqueId.equals(value.getParentUniqueId())) {
                result.add(value);
            }
        }
        return result;
    }

    @Override
    public void deleteAll() {
        store.clear();
        log.debug("Cleared all TestPrograms from in-memory store");
    }
}
