package com.functionscout.backend.service;

import com.functionscout.backend.dto.AddServiceDTO;
import com.functionscout.backend.dto.DashboardResponseDTO;
import com.functionscout.backend.dto.ServiceResponseDTO;
import com.functionscout.backend.exception.BadRequestException;
import com.functionscout.backend.model.Component;
import com.functionscout.backend.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    public void addService(final AddServiceDTO addServiceDTO) {
        validateAddServiceDTO(addServiceDTO);

        dashboardRepository.save(new Component(
                getServiceNameFromGithubUrl(addServiceDTO.getGithubURL()),
                addServiceDTO.getGithubURL())
        );
    }

    public List<DashboardResponseDTO> getALlServices() {
        return dashboardRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ServiceResponseDTO getOneService(final String serviceName) {
        final Component component = dashboardRepository.findComponentByName(serviceName)
                .orElseThrow(() -> new BadRequestException("No service exists with the name: " + serviceName));

        return new ServiceResponseDTO(
                component.getName(),
                component.getGithubUrl(),
                new ArrayList<>()
        );
    }

    private DashboardResponseDTO toDto(final Component component) {
        return new DashboardResponseDTO(
                component.getName(),
                component.getGithubUrl()
        );
    }

    private void validateAddServiceDTO(final AddServiceDTO addServiceDTO) {
        if (addServiceDTO == null) {
            throw new BadRequestException("Empty payload.");
        } else if (addServiceDTO.getGithubURL().isBlank()) {
            throw new BadRequestException("Github URL cannot be blank");
        }

        // Validate the github url using github API
    }

    private String getServiceNameFromGithubUrl(final String githubUrl) {
        final String[] urlComponents = githubUrl.split("/");
        return urlComponents[urlComponents.length - 1];
    }
}
