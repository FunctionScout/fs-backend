package com.functionscout.backend.controller;

import com.functionscout.backend.dto.AddServiceDTO;
import com.functionscout.backend.dto.DashboardResponseDTO;
import com.functionscout.backend.dto.ServiceResponseDTO;
import com.functionscout.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/services")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addService(@RequestBody AddServiceDTO addServiceDTO) {
        dashboardService.addService(addServiceDTO);
    }

    @GetMapping
    public List<DashboardResponseDTO> getAllServices() {
        return dashboardService.getALlServices();
    }

    @GetMapping("/{serviceName}")
    public ServiceResponseDTO getOneService(@PathVariable(name = "serviceName") String serviceName) {
        return dashboardService.getOneService(serviceName);
    }
}
