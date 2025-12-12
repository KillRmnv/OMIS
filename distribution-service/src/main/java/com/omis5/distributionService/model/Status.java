package com.omis5.distributionService.model;

import jakarta.persistence.*;
import lombok.Data;

@Table(name = "statuses")
@Data
@Entity
public class Status {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private long id;
    @Column(nullable = false)
    private String status;

}
