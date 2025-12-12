package com.omis5.distributionService.services;

import com.omis5.distributionService.model.Status;
import com.omis5.distributionService.repositories.StatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StatusService {

    private final StatusRepository statusRepository;

    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public Status save(Status status) {
        return statusRepository.save(status);
    }

    public Optional<Status> findById(Long id) {
        return statusRepository.findById(id);
    }

    public List<Status> findAll() {
        return statusRepository.findAll();
    }

    public void deleteById(Long id) {
        statusRepository.deleteById(id);
    }
}
