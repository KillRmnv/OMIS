package com.omis5.distributionService.services;

import com.omis5.distributionService.model.Status;
import com.omis5.distributionService.repositories.StatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StatusService {

    private final StatusRepository statusRepository;

    public Status save(Status status) {
        log.info("Saving status: {}", status.getStatus());
        Status saved = statusRepository.save(status);
        log.debug("Status saved with id: {}", saved.getId());
        return saved;
    }

    public Optional<Status> findById(Long id) {
        log.debug("Finding status by id: {}", id);
        Optional<Status> status = statusRepository.findById(id);
        if (status.isPresent()) {
            log.debug("Status found with id: {}", id);
        } else {
            log.warn("Status not found with id: {}", id);
        }
        return status;
    }

    public List<Status> findAll() {
        log.debug("Finding all statuses");
        List<Status> statuses = statusRepository.findAll();
        log.info("Found {} statuses", statuses.size());
        return statuses;
    }

    public void deleteById(Long id) {
        log.info("Deleting status with id: {}", id);
        statusRepository.deleteById(id);
        log.debug("Status deleted successfully");
    }
}
