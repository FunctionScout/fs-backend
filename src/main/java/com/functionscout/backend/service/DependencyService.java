package com.functionscout.backend.service;

import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.model.Dependency;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.repository.DependencyRepository;
import com.functionscout.backend.repository.JdbcDependencyRepository;
import com.functionscout.backend.repository.WebServiceRepository;
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

    public void createDependencies(final WebService webService,
                                   final List<DependencyDTO> webServiceDependencies) {
        jdbcDependencyRepository.saveAll(webServiceDependencies);

        final List<Dependency> dependencies = jdbcDependencyRepository.findAllDependenciesByNameAndVersion(webServiceDependencies);

        // TODO: Insert into webservicedependencies separately through a native query
        webService.setDependencies(new HashSet<>(dependencies));
        webServiceRepository.save(webService);
    }
}
