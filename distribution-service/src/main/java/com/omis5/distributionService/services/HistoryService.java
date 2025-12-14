package com.omis5.distributionService.services;

import com.omis5.distributionService.model.History;
import com.omis5.distributionService.repositories.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final HistoryRepository historyRepository;

    public History save(History history) {
        log.info("Saving history for taskId: {}", history.getTask().getId());
        History saved = historyRepository.save(history);
        log.debug("History saved with id: {}", saved.getId());
        return saved;
    }

    public Optional<History> findById(Long id) {
        log.debug("Finding history by id: {}", id);
        Optional<History> history = historyRepository.findById(id);
        if (history.isPresent()) {
            log.debug("History found with id: {}", id);
        } else {
            log.warn("History not found with id: {}", id);
        }
        return history;
    }

    public List<History> findAll() {
        log.debug("Finding all histories");
        List<History> histories = historyRepository.findAll();
        log.info("Found {} histories", histories.size());
        return histories;
    }

    public void deleteById(Long id) {
        log.info("Deleting history with id: {}", id);
        historyRepository.deleteById(id);
        log.debug("History deleted successfully");
    }

    public Optional<Set<History>> findByTaskId(Long taskId) {
        log.debug("Finding histories by taskId: {}", taskId);
        Optional<Set<History>> histories = historyRepository.findByTaskId(taskId);
        if (histories.isPresent()) {
            log.info("Found {} histories for taskId: {}", histories.get().size(), taskId);
        } else {
            log.warn("No histories found for taskId: {}", taskId);
        }
        return histories;
    }
}
