package testauto.controller;

import lombok.RequiredArgsConstructor;
import testauto.domain.TestExecution;
import testauto.domain.TestResult;
import testauto.domain.TestStatus;
import testauto.domain.TestSummary;
import testauto.service.TestExecutionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TestResultController {

    private final TestExecutionService testExecutionService;

    @PostMapping("/run")
    public String runTests(@RequestParam("testClass") String testClass) {
        // 비동기로 테스트 실행 시작
        String executionId = testExecutionService.submitTests(List.of(testClass), null);
        return "redirect:/results?executionId=" + executionId;
    }

    @GetMapping("/results")
    public String showResults(@RequestParam(required = false) String executionId, Model model) {
        if (executionId == null) {
            // executionId가 없으면 빈 결과 표시
            model.addAttribute("summary", new TestSummary());
            model.addAttribute("results", List.of());
            model.addAttribute("status", "NO_EXECUTION");
            return "test-results";
        }

        Optional<TestExecution> executionOpt = testExecutionService.getExecution(executionId);
        if (executionOpt.isEmpty()) {
            model.addAttribute("summary", new TestSummary());
            model.addAttribute("results", List.of());
            model.addAttribute("status", "NOT_FOUND");
            return "test-results";
        }

        TestExecution execution = executionOpt.get();
        model.addAttribute("executionId", executionId);
        model.addAttribute("status", execution.getStatus());

        if ("RUNNING".equals(execution.getStatus())) {
            // 아직 실행 중이면 빈 결과와 함께 상태 전달
            model.addAttribute("summary", new TestSummary());
            model.addAttribute("results", List.of());
        } else {
            // 완료되었으면 결과 조회
            List<TestResult> results = testExecutionService.getExecutionResultTree(executionId);
            TestSummary summary = calculateSummary(results);
            model.addAttribute("summary", summary);
            model.addAttribute("results", results);
        }

        return "test-results";
    }

    private TestSummary calculateSummary(List<TestResult> results) {
        TestSummary summary = new TestSummary();
        calculateSummaryRecursive(results, summary);
        return summary;
    }

    private void calculateSummaryRecursive(List<TestResult> results, TestSummary summary) {
        for (TestResult result : results) {
            if (result.getChildren().isEmpty()) {
                summary.incTotal();
                summary.addDuration(result.getDurationMillis());
                if (result.getStatus() == TestStatus.SUCCESS) {
                    summary.incSuccess();
                } else if (result.getStatus() == TestStatus.FAILED) {
                    summary.incFailed();
                } else if (result.getStatus() == TestStatus.SKIPPED) {
                    summary.incSkipped();
                }
            }
            calculateSummaryRecursive(result.getChildren(), summary);
        }
    }
}