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
public class WebServiceStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String githubUrl;

    private Short status;

    private String message;

    private Timestamp createDT;

    private Timestamp updateDT;

    @Autowired
    public WebServiceStatus(final String githubUrl, final Short status) {
        this.githubUrl = githubUrl;
        this.status = status;
        this.createDT = Timestamp.from(Instant.now());
        this.updateDT = Timestamp.from(Instant.now());
    }
}
