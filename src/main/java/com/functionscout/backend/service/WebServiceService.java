package com.functionscout.backend.service;

import com.functionscout.backend.dto.DashboardResponseDTO;
import com.functionscout.backend.dto.WebServiceDependencyResponse;
import com.functionscout.backend.dto.WebServiceRequest;
import com.functionscout.backend.dto.WebServiceResponse;
import com.functionscout.backend.enums.DependencyType;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.exception.BadRequestException;
import com.functionscout.backend.model.Dependency;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.repository.WebServiceRepository;
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
    private WebServiceProcessor webServiceProcessor;

    public void addService(final WebServiceRequest webServiceRequest) {
        validateAddServiceDTO(webServiceRequest);

        final int serviceRecords = webServiceRepository.countByGithubUrlAndStatuses(
                webServiceRequest.getGithubUrl(),
                List.of(Status.IN_PROGRESS.getCode(), Status.SUCCESS.getCode())
        );

        if (serviceRecords > 0) {
            throw new BadRequestException("Github url already exists");
        }

        final WebService webService = webServiceRepository.save(new WebService(webServiceRequest.getGithubUrl()));

        // Now add the githubUrl to a queue or pass it to an async function for processing
        webServiceProcessor.processGithubUrl(webService);
    }

    public List<DashboardResponseDTO> getAllWebServices() {
        return webServiceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // TODO: Remove transactional and use a native join query to fetch the result. Do not eager load the dependencies!!!
    @Transactional
    public WebServiceResponse getOneService(final String serviceId) {
        final WebService webService = webServiceRepository.findWebServiceByUuid(serviceId)
                .orElseThrow(() -> new BadRequestException("No service exists with the id: " + serviceId));
        final List<WebServiceDependencyResponse> webServiceDependencyResponses = webService.getDependencies()
                .stream()
                .map(this::toDto)
                .toList();

        return new WebServiceResponse(
                webService.getGithubUrl(),
                webServiceDependencyResponses
        );
    }

    private DashboardResponseDTO toDto(final WebService webService) {
        return new DashboardResponseDTO(
                webService.getUuid(),
                webService.getGithubUrl(),
                Status.getStatus(webService.getStatus()).name()
        );
    }

    private WebServiceDependencyResponse toDto(final Dependency dependency) {
        return new WebServiceDependencyResponse(
                dependency.getName(),
                dependency.getVersion(),
                DependencyType.getDependencyType(dependency.getType()).name()
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
