-- 테스트 결과 상세 테이블
CREATE TABLE IF NOT EXISTS bng000a.c_test_result (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     execution_id VARCHAR(36) NOT NULL,
    test_id VARCHAR(1000) NOT NULL,
    parent_test_id VARCHAR(1000),
    display_name VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    duration_millis BIGINT DEFAULT 0,
    error_message TEXT,
    stack_trace TEXT,
    stdout TEXT,
    INDEX c_test_result_idx1 (execution_id),
    FOREIGN KEY (execution_id) REFERENCES bng000a.c_test_execution(execution_id) ON DELETE CASCADE
    );
