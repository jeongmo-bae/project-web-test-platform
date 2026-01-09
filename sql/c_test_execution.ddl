-- 테스트 실행 이력 테이블
CREATE TABLE IF NOT EXISTS bng000a.c_test_execution
(
    execution_id          VARCHAR(36) PRIMARY KEY,
    started_at            DATETIME,
    finished_at           DATETIME,
    total_tests           INT    DEFAULT 0,
    success_count         INT    DEFAULT 0,
    failed_count          INT    DEFAULT 0,
    skipped_count         INT    DEFAULT 0,
    total_duration_millis BIGINT DEFAULT 0,
    INDEX c_test_execution_idx1 (started_at DESC)
);