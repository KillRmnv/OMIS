package com.omis5.distributionService.repositories;

import com.omis5.distributionService.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {
     Optional<Status> findByName(String name);
     Optional<Status> findById(Long id);
}
