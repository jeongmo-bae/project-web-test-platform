package testauto.repository;

import org.junit.platform.engine.UniqueId;
import testauto.domain.TestProgram;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestProgramRepository {
    void save(TestProgram testProgram);
    void saveAll(Collection<TestProgram> testPrograms);
    Optional<TestProgram> findByUniqueId(UniqueId uniqueId);
    List<TestProgram> findAll();
    List<TestProgram> findByParentId(UniqueId parentUniqueId);
    void deleteAll();
}
