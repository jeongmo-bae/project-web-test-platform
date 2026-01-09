package testauto.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import testauto.domain.TestExecution;
import testauto.domain.TestResult;
import testauto.domain.TestSummary;
import testauto.dto.ClassDetailDto;
import testauto.dto.TestExecutionRequest;
import testauto.dto.TestExecutionResponse;
import testauto.dto.TreeNodeDto;
import testauto.service.TestCatalogService;
import testauto.service.TestExecutionService;
import testauto.service.TestTreeService;
import testauto.service.SourceCodeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Validated
public class TestApiController {

    private final TestTreeService testTreeService;
    private final TestCatalogService testCatalogService;
    private final TestExecutionService testExecutionService;
    private final SourceCodeService sourceCodeService;

    @GetMapping("/tree")
    public ResponseEntity<TreeNodeDto> getTestTree() {
        TreeNodeDto tree = testTreeService.buildTree();
        return ResponseEntity.ok(tree);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refreshTestCatalog() {
        testCatalogService.refreshTestCatalog();
        TreeNodeDto tree = testTreeService.buildTree();
        return ResponseEntity.ok(new RefreshResponse("SUCCESS", "Test catalog refreshed", tree));
    }

    public record RefreshResponse(String status, String message, TreeNodeDto tree) {}

    @GetMapping("/class/{className}")
    public ResponseEntity<ClassDetailDto> getClassDetail(@PathVariable @NotBlank(message = "Class name cannot be blank") String className) {
        ClassDetailDto detail = testCatalogService.getClassDetail(className);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/run")
    public ResponseEntity<TestExecutionResponse> runTests(@Valid @RequestBody TestExecutionRequest request) {
        String executionId = UUID.randomUUID().toString();

        testExecutionService.runTests(request.getClassNames());

        return ResponseEntity.ok(TestExecutionResponse.builder()
                .executionId(executionId)
                .status("COMPLETED")
                .message("Tests executed successfully")
                .build());
    }

    @GetMapping("/results")
    public ResponseEntity<TestResultsResponse> getTestResults() {
        TestSummary summary = testExecutionService.getSummary();
        List<TestResult> results = testExecutionService.getTestTree();

        TestResultsResponse response = new TestResultsResponse(summary, results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/method/{className}/{methodName}/code")
    public ResponseEntity<MethodCodeResponse> getMethodCode(
            @PathVariable @NotBlank(message = "Class name cannot be blank") String className,
            @PathVariable @NotBlank(message = "Method name cannot be blank") String methodName) {

        String code = sourceCodeService.getMethodSourceCode(className, methodName);
        return ResponseEntity.ok(new MethodCodeResponse(code));
    }

    public record TestResultsResponse(TestSummary summary, List<TestResult> results) {}
    public record MethodCodeResponse(String code) {}

    @GetMapping("/executions")
    public ResponseEntity<List<TestExecution>> getRecentExecutions(
            @RequestParam(defaultValue = "20") int limit) {
        List<TestExecution> executions = testExecutionService.getRecentExecutions(limit);
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/executions/{executionId}/results")
    public ResponseEntity<TestResultsResponse> getExecutionResults(
            @PathVariable String executionId) {
        List<TestResult> results = testExecutionService.getExecutionResultTree(executionId);

        // Summary 계산
        TestSummary summary = new TestSummary();
        calculateSummary(results, summary);

        return ResponseEntity.ok(new TestResultsResponse(summary, results));
    }

    private void calculateSummary(List<TestResult> results, TestSummary summary) {
        for (TestResult result : results) {
            if (result.getChildren().isEmpty()) {
                summary.incTotal();
                summary.addDuration(result.getDurationMillis());
                switch (result.getStatus()) {
                    case SUCCESS -> summary.incSuccess();
                    case FAILED -> summary.incFailed();
                    case SKIPPED -> summary.incSkipped();
                }
            }
            calculateSummary(result.getChildren(), summary);
        }
    }
}
