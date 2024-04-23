package com.functionscout.backend.controller;

import com.functionscout.backend.dto.DashboardResponseDTO;
import com.functionscout.backend.dto.WebServiceRequest;
import com.functionscout.backend.dto.WebServiceStatusResponse;
import com.functionscout.backend.service.WebServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/services")
public class WebServiceController {
    @Autowired
    private WebServiceService webServiceService;

    // TODO: Pass in the time zone
    @PostMapping
    public void addService(@RequestBody WebServiceRequest webServiceRequest) {
        webServiceService.addService(webServiceRequest);
    }

    // TODO: Add pagination
    @GetMapping
    public List<DashboardResponseDTO> getAllServices() {
        return webServiceService.getAllWebServices();
    }

    @GetMapping("/status")
    public List<WebServiceStatusResponse> getAllServiceStatus() {
        return webServiceService.getAllWebServiceStatus();
    }

//    @GetMapping("/{serviceName}")
//    public ServiceResponseDTO getOneService(@PathVariable(name = "githubUrl") String githubUrl) {
//        return webServiceService.getOneService(githubUrl);
//    }
}
