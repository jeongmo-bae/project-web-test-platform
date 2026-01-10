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
    requester_ip          VARCHAR(45),
    class_names           TEXT,
    status                VARCHAR(20) DEFAULT 'RUNNING',
    INDEX c_test_execution_idx1 (started_at DESC),
    INDEX c_test_execution_idx2 (status)
);

-- 컬럼 추가용 (기존 테이블 업데이트 시)
# ALTER TABLE bng000a.c_test_execution ADD COLUMN requester_ip VARCHAR(45);
# ALTER TABLE bng000a.c_test_execution ADD COLUMN class_names TEXT;
# ALTER TABLE bng000a.c_test_execution ADD COLUMN status VARCHAR(20) DEFAULT 'RUNNING';