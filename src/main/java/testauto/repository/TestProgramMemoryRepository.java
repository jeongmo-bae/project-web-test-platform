package testauto.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.engine.UniqueId;
import org.springframework.stereotype.Repository;
import testauto.domain.TestProgram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
public class TestProgramMemoryRepository implements TestProgramRepository{
    // 스레드 안전한 in-memory 저장소
    private final ConcurrentMap<UniqueId, TestProgram> store = new ConcurrentHashMap<>();

    @Override
    public void save(TestProgram testProgram) {
        store.put(testProgram.getUniqueId(), testProgram);
        log.debug("Saved TestProgram : " + testProgram);
    }

    @Override
    public void saveAll(Collection<TestProgram> testPrograms) {
        for (TestProgram testProgram : testPrograms) {
            save(testProgram);
        }
    }

    @Override
    public Optional<TestProgram> findByUniqueId(UniqueId uniqueId) {
        return Optional.ofNullable(store.get(uniqueId));
    }

    @Override
    public List<TestProgram> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<TestProgram> findByParentId(UniqueId parentUniqueId) {
        List<TestProgram> result = new ArrayList<>();
        for (TestProgram value : store.values()) {
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
