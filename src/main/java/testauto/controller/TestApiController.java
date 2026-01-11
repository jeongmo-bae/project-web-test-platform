package testauto.controller;

import jakarta.servlet.http.HttpServletRequest;
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

import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<TestExecutionResponse> runTests(
            @Valid @RequestBody TestExecutionRequest request,
            HttpServletRequest httpRequest) {
        String requesterIp = getClientIp(httpRequest);

        // 비동기로 테스트 실행 시작, executionId 즉시 반환
        String executionId = testExecutionService.submitTests(request.getClassNames(), requesterIp);

        return ResponseEntity.ok(TestExecutionResponse.builder()
                .executionId(executionId)
                .status("RUNNING")
                .message("Test execution started")
                .build());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 여러 IP가 있을 경우 첫 번째 것만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @GetMapping("/method/code")
    public ResponseEntity<MethodCodeResponse> getMethodCode(
            @RequestParam @NotBlank(message = "uniqueId cannot be blank") String uniqueId) {

        String code = sourceCodeService.getMethodSourceCodeByUniqueId(uniqueId);
        return ResponseEntity.ok(new MethodCodeResponse(code));
    }

    public record TestResultsResponse(TestSummary summary, List<TestResult> results) {}
    public record MethodCodeResponse(String code) {}
    public record ServerTimeResponse(String today) {}

    @GetMapping("/server-time")
    public ResponseEntity<ServerTimeResponse> getServerTime() {
        String today = LocalDate.now().toString();
        return ResponseEntity.ok(new ServerTimeResponse(today));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<TestExecution>> getRecentExecutions(
            @RequestParam(defaultValue = "20") int limit) {
        List<TestExecution> executions = testExecutionService.getRecentExecutions(limit);
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<TestExecution> getExecution(@PathVariable String executionId) {
        return testExecutionService.getExecution(executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
