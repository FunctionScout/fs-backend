package com.functionscout.backend.service;

import com.functionscout.backend.dto.DashboardResponseDTO;
import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.dto.WebServiceRequest;
import com.functionscout.backend.dto.WebServiceResponse;
import com.functionscout.backend.dto.WebServiceStatusResponse;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.exception.BadRequestException;
import com.functionscout.backend.model.Dependency;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.model.WebServiceStatus;
import com.functionscout.backend.repository.WebServiceRepository;
import com.functionscout.backend.repository.WebServiceStatusRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WebServiceService {

    @Autowired
    private WebServiceRepository webServiceRepository;

    @Autowired
    private WebServiceStatusRepository webServiceStatusRepository;

    @Autowired
    private WebServiceProcessor webServiceProcessor;

    public void addService(final WebServiceRequest webServiceRequest) {
        validateAddServiceDTO(webServiceRequest);

        if (webServiceRepository.existsByGithubUrl(webServiceRequest.getGithubUrl())) {
            throw new BadRequestException("Github url already exists");
        }

        final WebServiceStatus webServiceStatus = webServiceStatusRepository.save(new WebServiceStatus(
                webServiceRequest.getGithubUrl(),
                Status.IN_PROGRESS.getCode()
        ));

        // Now add the githubUrl to a queue or pass it to an async function for processing
        webServiceProcessor.processGithubUrl(webServiceStatus);
    }

    public List<DashboardResponseDTO> getAllWebServices() {
        return webServiceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<WebServiceStatusResponse> getAllWebServiceStatus() {
        return webServiceStatusRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // TODO: Remove transactional and use a native join query to fetch the result. Do not use eager loading for dependencies!!!
    @Transactional
    public WebServiceResponse getOneService(final String serviceId) {
        final WebService webService = webServiceRepository.findWebServiceByUuid(serviceId)
                .orElseThrow(() -> new BadRequestException("No service exists with the id: " + serviceId));
        final List<DependencyDTO> dependencyDTOS = webService.getDependencies()
                .stream()
                .map(this::toDto)
                .toList();

        return new WebServiceResponse(
                webService.getGithubUrl(),
                dependencyDTOS
        );
    }

    private DashboardResponseDTO toDto(final WebService webService) {
        return new DashboardResponseDTO(
                webService.getUuid(),
                webService.getGithubUrl()
        );
    }

    private WebServiceStatusResponse toDto(final WebServiceStatus webServiceStatus) {
        return new WebServiceStatusResponse(
                webServiceStatus.getGithubUrl(),
                Status.getStatus(webServiceStatus.getStatus()).name(),
                webServiceStatus.getCreateDT().toLocalDateTime());
    }

    private DependencyDTO toDto(final Dependency dependency) {
        return new DependencyDTO(
                dependency.getName(),
                dependency.getVersion(),
                dependency.getType()
        );
    }

    private void validateAddServiceDTO(final WebServiceRequest webServiceRequest) {
        if (webServiceRequest == null) {
            throw new BadRequestException("Empty payload.");
        } else if (webServiceRequest.getGithubUrl().isBlank()) {
            throw new BadRequestException("Github URL cannot be blank");
        }

        // Validate the github url using github API
    }
}
