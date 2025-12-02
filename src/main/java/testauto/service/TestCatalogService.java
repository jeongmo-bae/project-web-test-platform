package testauto.service;

import org.junit.platform.engine.UniqueId;
import testauto.domain.TestProgram;

import java.util.List;

/**
 * 내부적으로 혹은 새로고침 버튼에 의해 , TestPlan 세우고 DB 저장 하게 하는게 더 나을거 같음
 * 사이드바 검색 조회 거래엔 DB 에서 찾아와 조립하는게 맞다
 *
 */
public interface TestCatalogService {
    void refreshTestCatalog();                // 디스커버리 → DB 갱신
    List<TestProgram> discoverAllTests();          // DB 조회
}
