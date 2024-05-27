package com.functionscout.backend.controller;

import com.functionscout.backend.dto.WebServiceResponse;
import com.functionscout.backend.dto.DependencyResponseDTO;
import com.functionscout.backend.dto.FunctionDetailResponseDTO;
import com.functionscout.backend.dto.FunctionResponseDTO;
import com.functionscout.backend.dto.WebServiceRequest;
import com.functionscout.backend.service.WebServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping
public class WebServiceController {
    @Autowired
    private WebServiceService webServiceService;

    @PostMapping("/services")
    public void addService(@RequestBody WebServiceRequest webServiceRequest) {
        webServiceService.addService(webServiceRequest);
    }

    // TODO: Add pagination
    @GetMapping("/services")
    public List<WebServiceResponse> getAllServices() {
        return webServiceService.getAllWebServices();
    }

    @GetMapping("/services/{serviceId}/dependencies")
    public Map<String, Object> getServiceDependencies(@PathVariable(name = "serviceId") String serviceId) {
        final List<DependencyResponseDTO> dependencyResponseDTOS = webServiceService.getServiceDependencies(serviceId);
        return Map.of("dependencies", dependencyResponseDTOS);
    }

    @GetMapping("/services/{serviceId}/functions")
    public Map<String, Object> getServiceFunctions(@PathVariable(name = "serviceId") String serviceId) {
        final List<FunctionResponseDTO> functionResponseDTOS = webServiceService.getServiceFunctions(serviceId);
        return Map.of("functions", functionResponseDTOS);
    }

    @GetMapping("/functions/{functionId}")
    public FunctionDetailResponseDTO getOneFunctionForService(@PathVariable(name = "functionId") String functionId) {
        return webServiceService.getOneFunctionForService(functionId);
    }
}
