-- 테스트 결과 상세 테이블 (DB2)
CREATE TABLE bng000a.c_test_result
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id    VARCHAR(36) NOT NULL,
    test_id         VARCHAR(1000) NOT NULL,
    parent_test_id  VARCHAR(1000),
    display_name    VARCHAR(500),
    status          VARCHAR(20) NOT NULL,
    duration_millis BIGINT DEFAULT 0,
    error_message   CLOB,
    stack_trace     CLOB,
    stdout          CLOB,
    CONSTRAINT fk_test_result_execution
        FOREIGN KEY (execution_id)
        REFERENCES bng000a.c_test_execution(execution_id)
        ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX c_test_result_idx1 ON bng000a.c_test_result (execution_id);

-- DROP TABLE bng000a.c_test_result;
