package testauto.repository;

import org.junit.platform.engine.UniqueId;
import testauto.domain.TestProgram;

import java.util.List;
import java.util.Optional;

public interface TestProgramRepository {
    void save(TestProgram testProgram);
    Optional<TestProgram> findByUniqueId(UniqueId uniqueId);
    List<TestProgram> findAll();
    void deleteAll();
}
