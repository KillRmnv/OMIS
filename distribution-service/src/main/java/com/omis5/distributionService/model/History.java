package com.omis5.distributionService.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="history")
public class History {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private long id;
    @JoinColumn(name = "task_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Task task;
    @Lob
    @Column(nullable = false)
    private String content;
}
