package com.functionscout.backend.service;

import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.model.Dependency;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.model.WebServiceStatus;
import com.functionscout.backend.repository.DependencyRepository;
import com.functionscout.backend.repository.JdbcDependencyRepository;
import com.functionscout.backend.repository.WebServiceRepository;
import com.functionscout.backend.repository.WebServiceStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class DependencyService {

    @Autowired
    private JdbcDependencyRepository jdbcDependencyRepository;

    @Autowired
    private WebServiceRepository webServiceRepository;

    @Autowired
    private DependencyRepository dependencyRepository;

    @Autowired
    private WebServiceStatusRepository webServiceStatusRepository;

    public void createDependencies(final WebServiceStatus webServiceStatus,
                                   final List<DependencyDTO> webServiceDependencies) {
        jdbcDependencyRepository.saveAllDependencies(webServiceDependencies);

        final List<Dependency> dependencies = jdbcDependencyRepository.getDependencies(webServiceDependencies);
        WebService webService = new WebService(webServiceStatus.getGithubUrl());
        webService.setDependencies(new HashSet<>(dependencies));
        webServiceRepository.save(webService);

        // Update the status
        webServiceStatusRepository.updateWebServiceStatus(
                Status.SUCCESS.getCode(),
                webServiceStatus.getId(),
                webServiceStatus.getGithubUrl()
        );
    }
}
