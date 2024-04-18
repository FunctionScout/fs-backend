package com.functionscout.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class DemoController {

    public DemoController() {

    }

    @GetMapping("/test")
    public String getDemoResponse() {
        return "Function Scout Backend";
    }
}
