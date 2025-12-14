package com.omis5.distributionService.controllers;

import com.omis5.distributionService.model.History;
import com.omis5.distributionService.services.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/distribution/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final HistoryService historyService;

    // Получить все записи истории
    @GetMapping
    public ResponseEntity<List<History>> getAllHistory() {
        log.info("GET /api/distribution/history - Getting all histories");
        List<History> histories = historyService.findAll();
        log.debug("Returning {} histories", histories.size());
        return ResponseEntity.ok(histories);
    }

    // Получить запись по id
    @GetMapping("/{id}")
    public ResponseEntity<History> getHistoryById(@PathVariable Long id) {
        log.info("GET /api/distribution/history/{} - Getting history by id", id);
        return historyService.findById(id)
                .map(history -> {
                    log.debug("History found with id: {}", id);
                    return ResponseEntity.ok(history);
                })
                .orElseGet(() -> {
                    log.warn("History not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    // Создать новую запись
    @PostMapping
    public ResponseEntity<History> createHistory(@RequestBody History history) {
        log.info("POST /api/distribution/history - Creating new history");
        History saved = historyService.save(history);
        log.info("History created successfully with id: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    // Обновить существующую запись
    @PutMapping("/{id}")
    public ResponseEntity<History> updateHistory(@PathVariable Long id, @RequestBody History history) {
        log.info("PUT /api/distribution/history/{} - Updating history", id);
        return historyService.findById(id)
                .map(existing -> {
                    existing.setContent(history.getContent());
                    existing.setTask(history.getTask());
                    History updated = historyService.save(existing);
                    log.info("History updated successfully");
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    log.warn("History not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    // Удалить запись по id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        log.info("DELETE /api/distribution/history/{} - Deleting history", id);
        if (historyService.findById(id).isPresent()) {
            historyService.deleteById(id);
            log.info("History deleted successfully");
            return ResponseEntity.noContent().build();
        } else {
            log.warn("History not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // Получить историю по ID задачи
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Optional<Set<History>>> getHistoryByTaskId(@PathVariable Long taskId) {
        log.info("GET /api/distribution/history/task/{} - Getting histories by taskId", taskId);
        Optional<Set<History>> histories = historyService.findByTaskId(taskId);
        if (histories.isPresent()) {
            log.debug("Found {} histories for taskId: {}", histories.get().size(), taskId);
        } else {
            log.warn("No histories found for taskId: {}", taskId);
        }
        return ResponseEntity.ok(histories);
    }
}
