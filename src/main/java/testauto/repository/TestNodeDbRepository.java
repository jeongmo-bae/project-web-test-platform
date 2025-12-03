package testauto.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import testauto.domain.TestNode;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TestNodeDbRepository implements TestNodeRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<TestNode> rowMapper = (rs, rowNum) -> {
        TestNode testNode = TestNode.builder()
                .uniqueId(rs.getString("unique_id"))
                .parentUniqueId(rs.getString("parent_unique_id"))
                .displayName(rs.getString("displayname"))
                .className(rs.getString("classname"))
                .type(rs.getString("type"))
                .build();
        return testNode;
    };


    @Override
    public void save(TestNode testNode) {
        String sql = "insert into project_testauto.C_TEST_NODE_CATALOG (unique_id, parent_unique_id, displayname, classname, type)\n" +
                "values (?,?,?,?,?);";
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, testNode.getUniqueId());
            ps.setString(2, testNode.getParentUniqueId());
            ps.setString(3, testNode.getDisplayName());
            ps.setString(4, testNode.getClassName());
            ps.setString(5, testNode.getType());
            return ps;
        });
    }

    @Override
    public void saveAll(Collection<TestNode> testNodes) {
        testNodes.forEach(this::save);
    }

    @Override
    public Optional<TestNode> findByUniqueId(String uniqueId) {
        return jdbcTemplate.query("select * from project_testauto.C_TEST_NODE_CATALOG where unique_id = ?", rowMapper, uniqueId).stream().findFirst();
    }

    @Override
    public List<TestNode> findAll() {
        return jdbcTemplate.query("select * from project_testauto.C_TEST_NODE_CATALOG", rowMapper);
    }

    @Override
    public List<TestNode> findByParentId(String parentUniqueId) {
        return jdbcTemplate.query("select * from project_testauto.C_TEST_NODE_CATALOG where parent_unique_id = ?", rowMapper, parentUniqueId);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("delete from project_testauto.C_TEST_NODE_CATALOG where 1=1");
    }
}
