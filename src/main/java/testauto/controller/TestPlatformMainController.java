package testauto.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import testauto.dto.TreeNodeDto;
import testauto.service.ProgramTreeService;

@Controller
@RequiredArgsConstructor
public class TestPlatformMainController {
    private final ProgramTreeService programTreeService;

    @GetMapping("/")
    public String root(Model model) {
        TreeNodeDto root = programTreeService.buildTree();
        model.addAttribute("rootPackageName", root.getName());
        model.addAttribute("testTree", root.getChildren());
        return "index"; // templates 기준이면 이름 맞춰서
    }
}