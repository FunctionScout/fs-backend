package com.functionscout.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "`Function`")
public class Function {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String uuid;

    private String name;

    private String signature;

    private String returnType;

    private boolean isUsed;

    private Timestamp createDT;

    private Timestamp updateDT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classId")
    private Class clazz;

    @Autowired
    public Function(final String name, final String signature, final String returnType, final Class clazz) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.signature = signature;
        this.returnType = returnType;
        this.isUsed = false;
        this.createDT = Timestamp.from(Instant.now());
        this.updateDT = Timestamp.from(Instant.now());
        this.clazz = clazz;
    }
}
