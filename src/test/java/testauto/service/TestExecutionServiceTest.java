package testauto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TestExecutionServiceTest {
    @Autowired
    private final TestExecutionServiceImpl testExecutionServiceImpl;

    public TestExecutionServiceTest(TestExecutionServiceImpl testExecutionServiceImpl) {
        this.testExecutionServiceImpl = testExecutionServiceImpl;
    }

}