package testauto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import testauto.domain.TestResult;
import testauto.domain.TestSummary;
import testauto.service.TestExecutionServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/testssss")
public class TestResultController {

    @Autowired
    private final TestExecutionServiceImpl testExecutionServiceImpl;

    public TestResultController(TestExecutionServiceImpl testExecutionServiceImpl) {
        this.testExecutionServiceImpl = testExecutionServiceImpl;
    }

    @PostMapping("/run")
    public String runTests(@RequestParam("testClass") String testClass) {
        // 예: autotest.demo.DummyCalcTest
        testExecutionServiceImpl.runTests(testClass);
        return "redirect:/tests/results";
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        TestSummary summary = testExecutionServiceImpl.getSummary();
        List<TestResult> results = testExecutionServiceImpl.getTestTree();

        model.addAttribute("summary", summary);
        model.addAttribute("results", results);

        return "test-results";
    }

    @GetMapping
    public String index() {
        return "test-index"; // 간단히 클래스명 입력하는 폼
    }
}