package com.functionscout.backend.service;

import com.functionscout.backend.dto.DependencyData;
import com.functionscout.backend.dto.DependencyResponseDTO;
import com.functionscout.backend.dto.DependentWebServicesDTO;
import com.functionscout.backend.dto.FunctionDetailResponseDTO;
import com.functionscout.backend.dto.FunctionResponseDTO;
import com.functionscout.backend.dto.WebServiceDependencyDTO;
import com.functionscout.backend.dto.WebServiceFunctionDependencyDTO;
import com.functionscout.backend.dto.WebServiceRequest;
import com.functionscout.backend.dto.WebServiceResponse;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.exception.BadRequestException;
import com.functionscout.backend.model.Function;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.parser.GithubUrlParser;
import com.functionscout.backend.parser.WebServiceParser;
import com.functionscout.backend.repository.FunctionRepository;
import com.functionscout.backend.repository.JdbcDependencyRepository;
import com.functionscout.backend.repository.JdbcFunctionRepository;
import com.functionscout.backend.repository.JdbcWebServiceFunctionDependencyRepository;
import com.functionscout.backend.repository.WebServiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WebServiceService {

    @Autowired
    private WebServiceRepository webServiceRepository;

    @Autowired
    private WebServiceParser webServiceParser;

    @Autowired
    private JdbcFunctionRepository jdbcFunctionRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private JdbcDependencyRepository jdbcDependencyRepository;

    @Autowired
    private GithubUrlParser githubUrlParser;

    @Autowired
    private JdbcWebServiceFunctionDependencyRepository jdbcWebServiceFunctionDependencyRepository;

    public void addService(final WebServiceRequest webServiceRequest) {
        validateAddServiceDTO(webServiceRequest);

        final int inProgressServicesCount = webServiceRepository.countByGithubUrlAndStatus(
                webServiceRequest.getGithubUrl(),
                Status.IN_PROGRESS.getCode()
        );

        if (inProgressServicesCount > 0) {
            throw new BadRequestException("The githubUrl is currently being processed. Try again in sometime...");
        }

        Optional<WebService> webService = webServiceRepository.findWebServiceByGithubUrlAndStatus(
                webServiceRequest.getGithubUrl(),
                Status.SUCCESS.getCode()
        );

        if (webService.isEmpty()) {
            webService = Optional.of(webServiceRepository.save(new WebService(webServiceRequest.getGithubUrl())));
        }

        webServiceParser.processGithubUrl(webService.get());
    }

    public List<WebServiceResponse> getAllWebServices() {
        return webServiceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<DependencyResponseDTO> getServiceDependencies(final String serviceId) {
        final WebService webService = findServiceIfExists(serviceId);

        final List<WebServiceDependencyDTO> webServiceDependencies =
                jdbcDependencyRepository.findAllWebServiceDependenciesByServiceId(webService.getId());
        final Map<Object, WebServiceDependencyDTO> webServiceDependencyDTOMap = webServiceDependencies
                .stream()
                .collect(Collectors.toMap(
                        webServiceDependencyDTO -> List.of(
                                webServiceDependencyDTO.getServiceId(),
                                webServiceDependencyDTO.getDependencyId()
                        ),
                        java.util.function.Function.identity())
                );
        final Map<Object, List<FunctionResponseDTO>> webServiceFunctionDependencies =
                jdbcDependencyRepository.findAllUsedWebServiceFunctionDependencies(webServiceDependencies)
                        .stream()
                        .collect(Collectors.groupingBy(
                                webServiceFunctionDependencyDTO -> List.of(
                                        webServiceFunctionDependencyDTO.getServiceId(),
                                        webServiceFunctionDependencyDTO.getDependencyId()
                                ),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        webServiceFunctionDependencyDTOS -> webServiceFunctionDependencyDTOS
                                                .stream()
                                                .map(WebServiceFunctionDependencyDTO::getFunctionResponseDTO)
                                                .collect(Collectors.toList())
                                )
                        ));
        final List<DependencyResponseDTO> dependencyResponseDTOS = new ArrayList<>();

        for (Object key : webServiceDependencyDTOMap.keySet()) {
            final DependencyData dependencyData = webServiceDependencyDTOMap.get(key).getDependencyData();
            final DependencyResponseDTO dependencyResponseDTO = new DependencyResponseDTO(
                    dependencyData.getName(),
                    dependencyData.getVersion(),
                    dependencyData.getType(),
                    new ArrayList<>()
            );

            if (webServiceFunctionDependencies.containsKey(key)) {
                dependencyResponseDTO.setUsedFunctions(webServiceFunctionDependencies.get(key));
            }

            dependencyResponseDTOS.add(dependencyResponseDTO);
        }

        return dependencyResponseDTOS;
    }

    public List<FunctionResponseDTO> getFunctions(final String serviceId,
                                                  final boolean isUsed) {
        final WebService webService = findServiceIfExists(serviceId);
        final List<FunctionResponseDTO> functions = jdbcFunctionRepository.findAllUsedFunctionsByServiceId(
                webService.getId(),
                isUsed
        );

        if (functions != null && functions.isEmpty()) {
            return new ArrayList<>();
        }

        return functions;
    }

    // TODO: Remove transactional and use a native join query to fetch the result. Do not eager load the dependencies!!!
    @Transactional
    public FunctionDetailResponseDTO getOneFunctionForService(final String functionId) {
        final Optional<Function> optionalFunction = functionRepository.findFunctionByUuid(functionId);

        if (optionalFunction.isEmpty()) {
            return new FunctionDetailResponseDTO();
        }

        final Function function = optionalFunction.get();
        final List<DependentWebServicesDTO> dependentWebServicesDTOS =
                jdbcWebServiceFunctionDependencyRepository.findAllDependentServicesForFunctionId(function.getId());

        return new FunctionDetailResponseDTO(function, dependentWebServicesDTOS);
    }

    private WebServiceResponse toDto(final WebService webService) {
        return new WebServiceResponse(
                webService.getUuid(),
                webService.getGithubUrl(),
                Status.getStatus(webService.getStatus()).name()
        );
    }

    private void validateAddServiceDTO(final WebServiceRequest webServiceRequest) {
        if (webServiceRequest == null) {
            throw new BadRequestException("Empty payload.");
        } else if (webServiceRequest.getGithubUrl().isBlank()) {
            throw new BadRequestException("Github URL cannot be blank");
        } else if (!githubUrlParser.isValid(webServiceRequest.getGithubUrl())) {
            throw new BadRequestException("Required format for the Github URL is: https://github.com/OWNER/REPOSITORY.git");
        }
    }

    private WebService findServiceIfExists(final String serviceId) {
        return webServiceRepository.findWebServiceByUuid(serviceId)
                .orElseThrow(() -> new BadRequestException("No service exists with the id: " + serviceId));
    }
}
