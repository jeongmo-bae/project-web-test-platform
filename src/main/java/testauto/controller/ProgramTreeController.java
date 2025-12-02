package testauto.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import testauto.service.TestCatalogService;

@Controller
@RequiredArgsConstructor
public class ProgramTreeController {
    private final TestCatalogService testCatalogService;

    // 루트 → /tests 화면 재사용
    @GetMapping("/")
    public String root(Model model) {
        return tests(model);
    }

    @GetMapping("/tests")
    public String tests(Model model) {
        // 필요하면 여기서 discoverAllTests() 먼저 호출
        // testDiscoveryService.discoverAllTests();

        model.addAttribute("treeNodes", testCatalogService.buildTree());
        return "tests/index"; // templates/tests/index.html
    }
}