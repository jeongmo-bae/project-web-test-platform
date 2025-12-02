package testauto.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import testauto.service.ProgramTreeService;

@Controller
@RequiredArgsConstructor
public class ProgramTreeController {
    private final ProgramTreeService programTreeService;

    // 루트 → /tests 화면 재사용
//    @GetMapping("/")
//    public String root(Model model) {
//        return tests(model);
//    }

    @GetMapping("/tests")
    public String tests(Model model) {
        model.addAttribute("treeNodes", programTreeService.buildTree());
        return "tests/home"; // templates/tests/home.html
    }
}