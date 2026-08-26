package com.localpaymap.controller.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapPageController {

    private final String naverClientId;

    public MapPageController(@Value("${naver.client-id:}") String naverClientId) {
        this.naverClientId = naverClientId;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("naverClientId", naverClientId);
        return "index";
    }
}
