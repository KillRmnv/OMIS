package com.omis5.distributionService.repositories;

import com.omis5.distributionService.model.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface HistoryRepository extends JpaRepository<History, Long> {
    Optional<Set<History>> findByUserId(long userId);

    Optional<Set<History>> findByTaskId(long taskId);
}
