package com.functionscout.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    public DemoController() {

    }

    @GetMapping("/demo")
    public String getDemoResponse() {
        return "This is function scout demo backend";
    }
}
