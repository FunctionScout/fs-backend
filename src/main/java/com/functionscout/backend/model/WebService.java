package com.functionscout.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class WebService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String uuid;

    private String githubUrl;

    private Timestamp createDT;

    @ManyToMany
    @JoinTable(name = "WebServiceDependency",
            joinColumns = @JoinColumn(name = "serviceId"),
            inverseJoinColumns = @JoinColumn(name = "dependencyId"))
    private Set<Dependency> dependencies = new HashSet<>();

    @Autowired
    public WebService(final String githubUrl) {
        this.uuid = UUID.randomUUID().toString();
        this.githubUrl = githubUrl;
        this.createDT = Timestamp.from(Instant.now());
    }
}
