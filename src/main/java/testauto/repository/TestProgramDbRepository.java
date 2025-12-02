package testauto.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import testauto.domain.TestProgram;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TestProgramDbRepository implements TestProgramRepository{
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TestProgramDbRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<TestProgram> rowMapper = (rs, rowNum) -> {
        TestProgram testProgram = TestProgram.builder()
                .uniqueId(rs.getString("unique_id"))
                .parentUniqueId(rs.getString("parent_unique_id"))
                .displayName(rs.getString("displayname"))
                .className(rs.getString("classname"))
                .type(rs.getString("type"))
                .build();
        return testProgram;
    };


    @Override
    public void save(TestProgram testProgram) {
        String sql = "insert into project_testauto.C_TEST_PROGRAM_CATALOG (unique_id, parent_unique_id, displayname, classname, type)\n" +
                "values (?,?,?,?,?);";
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, testProgram.getUniqueId());
            ps.setString(2, testProgram.getParentUniqueId());
            ps.setString(3, testProgram.getDisplayName());
            ps.setString(4, testProgram.getClassName());
            ps.setString(5, testProgram.getType());
            return ps;
        });
    }

    @Override
    public void saveAll(Collection<TestProgram> testPrograms) {
        testPrograms.forEach(this::save);
    }

    @Override
    public Optional<TestProgram> findByUniqueId(String uniqueId) {
        return Optional.empty();
    }

    @Override
    public List<TestProgram> findAll() {
        return List.of();
    }

    @Override
    public List<TestProgram> findByParentId(String parentUniqueId) {
        return List.of();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("delete from project_testauto.C_TEST_PROGRAM_CATALOG where 1=1");
    }
}
