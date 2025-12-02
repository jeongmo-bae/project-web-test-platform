package testauto.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import testauto.service.ProgramTreeService;

@Controller
@RequiredArgsConstructor
public class TestPlatformMainController {
    private final ProgramTreeService programTreeService;

    @GetMapping("~")
    public void root(Model model) {
        model.addAttribute("treeNodes", programTreeService.buildTree());
    }
}