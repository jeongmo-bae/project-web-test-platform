-- 데이터베이스 생성 (DB2에서는 CREATE DATABASE 대신 CREATE SCHEMA 사용)
-- CREATE SCHEMA bng000a;

-- 테스트 노드 카탈로그 테이블 (DB2)
CREATE TABLE bng000a.C_TEST_NODE_CATALOG
(
    unique_id        VARCHAR(200) NOT NULL PRIMARY KEY,
    parent_unique_id VARCHAR(200),
    displayname      VARCHAR(200),
    classname        VARCHAR(200),
    type             VARCHAR(20),
    updatedat        TIMESTAMP DEFAULT CURRENT TIMESTAMP
);

-- updatedat 자동 갱신을 위한 트리거 (필요 시)
-- CREATE TRIGGER bng000a.trg_test_node_catalog_update
-- NO CASCADE BEFORE UPDATE ON bng000a.C_TEST_NODE_CATALOG
-- REFERENCING NEW AS N
-- FOR EACH ROW
-- SET N.updatedat = CURRENT TIMESTAMP;

-- DROP TABLE bng000a.C_TEST_NODE_CATALOG;

-- 조회 쿼리 예시
-- SELECT unique_id,
--        parent_unique_id,
--        displayname,
--        classname,
--        type,
--        updatedat
-- FROM bng000a.C_TEST_NODE_CATALOG;
