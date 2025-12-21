package testauto.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import testauto.dto.TreeNodeDto;
import testauto.service.TestCatalogService;
import testauto.service.TestTreeService;

@Controller
@RequiredArgsConstructor
public class TestPlatformMainController {
    private final TestTreeService testTreeService;
    private final TestCatalogService testCatalogService;

    @GetMapping("/")
    public String root(Model model) {
        TreeNodeDto root = testTreeService.buildTree();
        model.addAttribute("rootPackageName", root.getName());
        model.addAttribute("testTree", root.getChildren());
        return "index"; // templates 기준이면 이름 맞춰서
    }
//    @GetMapping("/refresh/catalog")
//    public void refreshCatalog(Model model){
//        testCatalogService.refreshTestCatalog();
//        LastUpdatedTimeDto lastUpdatedTime = testCatalogService.getLastUpdatedTime();
//        model.addAttribute("updatedAt", lastUpdatedTime.getLastUpdatedTime());
//    }
}