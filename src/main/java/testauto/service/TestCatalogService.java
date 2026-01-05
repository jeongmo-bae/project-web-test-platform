package testauto.service;

import testauto.domain.TestNode;
import testauto.dto.ClassDetailDto;

import java.util.List;

/**
 * 내부적으로 혹은 새로고침 버튼에 의해 , TestPlan 세우고 DB 저장 하게 하는게 더 나을거 같음
 * 사이드바 검색 조회 거래엔 DB 에서 찾아와 조립하는게 맞다
 *
 */
public interface TestCatalogService {
    void refreshTestCatalog();                // 디스커버리 → DB 갱신
    List<TestNode> discoverAllTests();          // DB 조회
    ClassDetailDto getClassDetail(String className);  // 클래스 상세 정보 조회
}
