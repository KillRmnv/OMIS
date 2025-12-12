package com.omis5.distributionService.model;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
@Table(name = "tasks")
@Data
@Entity
public class Task {
    @Id
    @Column(nullable=false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private long id;
    @Column(nullable = false)
    private long creatorId;
    @Column(nullable = false)
    private long docsId;
    @JoinColumn(name="status_id",nullable=false)
    @ManyToOne(fetch=FetchType.LAZY)
    private Status status;
    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    private List<History> history;
    @Column(nullable = false)
    private long currentTechnicalWriterId;
}
