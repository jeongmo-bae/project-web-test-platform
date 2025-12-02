package testauto.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import testauto.repository.TestProgramRepository;

@SpringBootTest
class TestDiscoveryServiceImplTest {
    @Autowired
    private TestProgramRepository testProgramRepository;

    @Test
    @DisplayName("TestProgram 전부 저장 테스트")
    void saveTest(){
        //given
        TestCatalogServiceImpl testDiscoveryServiceImpl = new TestCatalogServiceImpl(testProgramRepository);
        System.out.println("1.최초 TestProgram 빈거 확인");
        Assertions.assertThat(testProgramRepository.findAll()).isEmpty();
        for (int i = 0; i < testProgramRepository.findAll().size(); i++) {
            System.out.println("testProgramRepository.findAll().get("+ i + ") = " + testProgramRepository.findAll().get(i));
        }
        //when
        testDiscoveryServiceImpl.discoverAllTests();
        //then
        System.out.println("2.전체 조회후 저장 확인");
        Assertions.assertThat(testProgramRepository.findAll().size()).isEqualTo(4);
        for (int i = 0; i < testProgramRepository.findAll().size(); i++) {
            System.out.println("testProgramRepository.findAll().get("+ i + ") = " + testProgramRepository.findAll().get(i));
        }
    }

}