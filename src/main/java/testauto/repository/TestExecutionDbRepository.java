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
                    .build();

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
                (execution_id, started_at, finished_at, total_tests, success_count, failed_count, skipped_count, total_duration_millis)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
            return ps;
        });
    }

    @Override
    public void updateExecution(TestExecution execution) {
        String sql = """
                UPDATE bng000a.c_test_execution
                SET finished_at = ?, total_tests = ?, success_count = ?, failed_count = ?, skipped_count = ?, total_duration_millis = ?
                WHERE execution_id = ?
                """;
        jdbcTemplate.update(sql,
                execution.getFinishedAt() != null ? Timestamp.valueOf(execution.getFinishedAt()) : null,
                execution.getTotalTests(),
                execution.getSuccessCount(),
                execution.getFailedCount(),
                execution.getSkippedCount(),
                execution.getTotalDurationMillis(),
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
        return jdbcTemplate.query(
                "SELECT * FROM bng000a.c_test_execution ORDER BY started_at DESC LIMIT ?",
                executionRowMapper, limit
        );
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
                "SELECT * FROM bng000a.c_test_execution ORDER BY started_at DESC LIMIT 1",
                executionRowMapper
        ).stream().findFirst();
    }
}
