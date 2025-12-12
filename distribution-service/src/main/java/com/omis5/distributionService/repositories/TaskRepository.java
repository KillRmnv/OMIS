package com.omis5.distributionService.repositories;

import com.omis5.distributionService.model.Status;
import com.omis5.distributionService.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByCreatorId(long creatorId);

    List<Task> findByStatus(Status statusId);
}
