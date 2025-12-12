package com.omis5.distributionService.services;

import com.omis5.distributionService.model.History;
import com.omis5.distributionService.repositories.HistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HistoryService {

    private final HistoryRepository historyRepository;

    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public History save(History history) {
        return historyRepository.save(history);
    }

    public Optional<History> findById(Long id) {
        return historyRepository.findById(id);
    }

    public List<History> findAll() {
        return historyRepository.findAll();
    }

    public void deleteById(Long id) {
        historyRepository.deleteById(id);
    }

    public Optional<Set<History>> findByTaskId(Long taskId) {
        return historyRepository.findByTaskId(taskId);
    }
}
