package testauto.repository;

import org.junit.platform.engine.UniqueId;
import org.springframework.stereotype.Repository;
import testauto.domain.TestProgram;

import java.util.List;
import java.util.Optional;

@Repository
public class TestProgramMemoryRepository implements TestProgramRepository{
    private final List<TestProgram> programs = List.of();

    @Override
    public void save(TestProgram testProgram) {

    }

    @Override
    public Optional<TestProgram> findByUniqueId(UniqueId uniqueId) {
        return Optional.empty();
    }

    @Override
    public List<TestProgram> findAll() {
        return List.of();
    }

    @Override
    public void deleteAll() {

    }
}
