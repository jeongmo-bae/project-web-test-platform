package testauto.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import testauto.domain.TestExecution;
import testauto.domain.TestResultRecord;
import testauto.domain.TestStatus;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TestExecutionDbRepository implements TestExecutionRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TestExecution> executionRowMapper = (rs, rowNum) ->
            TestExecution.builder()
                    .executionId(rs.getString("execution_id"))
                    .startedAt(rs.getTimestamp("started_at") != null ?
                            rs.getTimestamp("started_at").toLocalDateTime() : null)
                    .finishedAt(rs.getTimestamp("finished_at") != null ?
                            rs.getTimestamp("finished_at").toLocalDateTime() : null)
                    .totalTests(rs.getInt("total_tests"))
                    .successCount(rs.getInt("success_count"))
                    .failedCount(rs.getInt("failed_count"))
                    .skippedCount(rs.getInt("skipped_count"))
                    .totalDurationMillis(rs.getLong("total_duration_millis"))
                    .requesterIp(rs.getString("requester_ip"))
                    .requesterName(getStringOrNull(rs, "requester_name"))
                    .classNames(rs.getString("class_names"))
                    .status(rs.getString("status"))
                    .build();

    private String getStringOrNull(java.sql.ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (java.sql.SQLException e) {
            return null;
        }
    }

    private final RowMapper<TestResultRecord> resultRowMapper = (rs, rowNum) ->
            TestResultRecord.builder()
                    .id(rs.getLong("id"))
                    .executionId(rs.getString("execution_id"))
                    .testId(rs.getString("test_id"))
                    .parentTestId(rs.getString("parent_test_id"))
                    .displayName(rs.getString("display_name"))
                    .status(TestStatus.valueOf(rs.getString("status")))
                    .durationMillis(rs.getLong("duration_millis"))
                    .errorMessage(rs.getString("error_message"))
                    .stackTrace(rs.getString("stack_trace"))
                    .stdout(rs.getString("stdout"))
                    .build();

    @Override
    public void saveExecution(TestExecution execution) {
        String sql = """
                INSERT INTO bng000a.c_test_execution
                (execution_id, started_at, finished_at, total_tests, success_count, failed_count, skipped_count, total_duration_millis, requester_ip, class_names, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, execution.getExecutionId());
            ps.setTimestamp(2, execution.getStartedAt() != null ?
                    Timestamp.valueOf(execution.getStartedAt()) : null);
            ps.setTimestamp(3, execution.getFinishedAt() != null ?
                    Timestamp.valueOf(execution.getFinishedAt()) : null);
            ps.setInt(4, execution.getTotalTests());
            ps.setInt(5, execution.getSuccessCount());
            ps.setInt(6, execution.getFailedCount());
            ps.setInt(7, execution.getSkippedCount());
            ps.setLong(8, execution.getTotalDurationMillis());
            ps.setString(9, execution.getRequesterIp());
            ps.setString(10, execution.getClassNames());
            ps.setString(11, execution.getStatus() != null ? execution.getStatus() : "RUNNING");
            return ps;
        });
    }

    @Override
    public void updateExecution(TestExecution execution) {
        String sql = """
                UPDATE bng000a.c_test_execution
                SET finished_at = ?, total_tests = ?, success_count = ?, failed_count = ?, skipped_count = ?, total_duration_millis = ?, status = ?
                WHERE execution_id = ?
                """;
        jdbcTemplate.update(sql,
                execution.getFinishedAt() != null ? Timestamp.valueOf(execution.getFinishedAt()) : null,
                execution.getTotalTests(),
                execution.getSuccessCount(),
                execution.getFailedCount(),
                execution.getSkippedCount(),
                execution.getTotalDurationMillis(),
                execution.getStatus() != null ? execution.getStatus() : "COMPLETED",
                execution.getExecutionId());
    }

    @Override
    public void saveResult(TestResultRecord result) {
        String sql = """
                INSERT INTO bng000a.c_test_result
                (execution_id, test_id, parent_test_id, display_name, status, duration_millis, error_message, stack_trace, stdout)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, result.getExecutionId());
            ps.setString(2, result.getTestId());
            ps.setString(3, result.getParentTestId());
            ps.setString(4, result.getDisplayName());
            ps.setString(5, result.getStatus().name());
            ps.setLong(6, result.getDurationMillis());
            ps.setString(7, result.getErrorMessage());
            ps.setString(8, result.getStackTrace());
            ps.setString(9, result.getStdout());
            return ps;
        });
    }

    @Override
    public void saveAllResults(List<TestResultRecord> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO bng000a.c_test_result
                (execution_id, test_id, parent_test_id, display_name, status, duration_millis, error_message, stack_trace, stdout)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, results, results.size(),
                (ps, result) -> {
                    ps.setString(1, result.getExecutionId());
                    ps.setString(2, result.getTestId());
                    ps.setString(3, result.getParentTestId());
                    ps.setString(4, result.getDisplayName());
                    ps.setString(5, result.getStatus().name());
                    ps.setLong(6, result.getDurationMillis());
                    ps.setString(7, result.getErrorMessage());
                    ps.setString(8, result.getStackTrace());
                    ps.setString(9, result.getStdout());
                });
    }

    @Override
    public Optional<TestExecution> findExecutionById(String executionId) {
        return jdbcTemplate.query(
                "SELECT * FROM bng000a.c_test_execution WHERE execution_id = ?",
                executionRowMapper, executionId
        ).stream().findFirst();
    }

    @Override
    public List<TestExecution> findAllExecutions() {
        return jdbcTemplate.query(
                "SELECT * FROM bng000a.c_test_execution ORDER BY started_at DESC",
                executionRowMapper
        );
    }

    @Override
    public List<TestExecution> findRecentExecutions(int limit) {
        String sql = """
                SELECT e.*, m.EMPNM AS requester_name
                FROM bng000a.c_test_execution e
                LEFT JOIN bng000a.c_morning_monitor_manager m ON e.requester_ip = m.EMPIP
                ORDER BY e.started_at DESC
                FETCH FIRST ? ROWS ONLY
                """;
        return jdbcTemplate.query(sql, executionRowMapper, limit);
    }

    @Override
    public List<TestResultRecord> findResultsByExecutionId(String executionId) {
        return jdbcTemplate.query(
                "SELECT * FROM bng000a.c_test_result WHERE execution_id = ?",
                resultRowMapper, executionId
        );
    }

    @Override
    public Optional<TestExecution> findLatestExecution() {
        return jdbcTemplate.query(
                "SELECT * FROM bng000a.c_test_execution ORDER BY started_at DESC FETCH FIRST 1 ROW ONLY",
                executionRowMapper
        ).stream().findFirst();
    }

    @Override
    public Map<String, Object> getTodayStats() {
        String sql = """
                SELECT
                    COUNT(*) as "total_executions",
                    COALESCE(SUM(total_tests), 0) as "total_tests",
                    COALESCE(SUM(success_count), 0) as "success_count",
                    COALESCE(SUM(failed_count), 0) as "failed_count",
                    COALESCE(SUM(skipped_count), 0) as "skipped_count"
                FROM bng000a.c_test_execution
                WHERE started_at >= CURRENT DATE
                  AND started_at < CURRENT DATE + 1 DAY
                  AND status <> 'RUNNING'
                """;
        return jdbcTemplate.queryForMap(sql);
    }

    @Override
    public List<Map<String, Object>> getWeeklyTrend() {
        String sql = """
                SELECT
                    DATE(started_at) as "date",
                    COUNT(*) as "executions",
                    COALESCE(SUM(success_count), 0) as "success_count",
                    COALESCE(SUM(failed_count), 0) as "failed_count"
                FROM bng000a.c_test_execution
                WHERE started_at >= CURRENT DATE - 6 DAYS AND status <> 'RUNNING'
                GROUP BY DATE(started_at)
                ORDER BY DATE(started_at)
                """;
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> getRecentFailures(int limit) {
        String sql = """
                SELECT
                    r.display_name as "display_name",
                    r.error_message as "error_message",
                    e.started_at as "started_at",
                    e.execution_id as "execution_id"
                FROM bng000a.c_test_result r
                JOIN bng000a.c_test_execution e ON r.execution_id = e.execution_id
                WHERE r.status = 'FAILED'
                ORDER BY e.started_at DESC
                FETCH FIRST ? ROWS ONLY
                """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    @Override
    public int getTotalTestClasses() {
        String sql = "SELECT COUNT(DISTINCT classname) FROM bng000a.C_TEST_NODE_CATALOG WHERE type = 'CONTAINER' AND classname IS NOT NULL AND unique_id NOT LIKE '%[nested-class:%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public boolean isAuthorizedUser(String ip) {
        String sql = "SELECT COUNT(*) FROM bng000a.c_morning_monitor_manager WHERE EMPIP = ? AND ACTIVE_YN = '1'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ip);
        return count != null && count > 0;
    }
}
