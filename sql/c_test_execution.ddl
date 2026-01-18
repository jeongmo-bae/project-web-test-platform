-- 테스트 실행 이력 테이블 (DB2)
CREATE TABLE bng000a.c_test_execution
(
    execution_id          VARCHAR(36) NOT NULL PRIMARY KEY,
    started_at            TIMESTAMP,
    finished_at           TIMESTAMP,
    total_tests           INTEGER DEFAULT 0,
    success_count         INTEGER DEFAULT 0,
    failed_count          INTEGER DEFAULT 0,
    skipped_count         INTEGER DEFAULT 0,
    total_duration_millis BIGINT DEFAULT 0,
    requester_ip          VARCHAR(45),
    class_names           CLOB,
    status                VARCHAR(20) DEFAULT 'RUNNING'
);

-- 인덱스 생성
CREATE INDEX c_test_execution_idx1 ON bng000a.c_test_execution (started_at DESC);
CREATE INDEX c_test_execution_idx2 ON bng000a.c_test_execution (status);

-- 컬럼 추가용 (기존 테이블 업데이트 시)
-- ALTER TABLE bng000a.c_test_execution ADD COLUMN requester_ip VARCHAR(45);
-- ALTER TABLE bng000a.c_test_execution ADD COLUMN class_names CLOB;
-- ALTER TABLE bng000a.c_test_execution ADD COLUMN status VARCHAR(20) DEFAULT 'RUNNING';

-- DROP TABLE bng000a.c_test_execution;
select * from bng000a.c_test_execution;