package com.localpaymap.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/stores")
    public String stores() {
        return "admin/stores";
    }
}
