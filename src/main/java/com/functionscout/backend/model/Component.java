package com.functionscout.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String name;

    private String githubUrl;

    private Timestamp createDT;

    @Autowired
    public Component(final String name, final String githubUrl) {
        this.name = name;
        this.githubUrl = githubUrl;
        this.createDT = Timestamp.from(Instant.now());
    }
}
