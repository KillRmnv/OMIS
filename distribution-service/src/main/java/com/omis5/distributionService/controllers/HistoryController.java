package com.omis5.distributionService.controllers;

import com.omis5.distributionService.model.History;
import com.omis5.distributionService.services.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/distribution/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    // Получить все записи истории
    @GetMapping
    public ResponseEntity<List<History>> getAllHistory() {
        List<History> histories = historyService.findAll();
        return ResponseEntity.ok(histories);
    }

    // Получить запись по id
    @GetMapping("/{id}")
    public ResponseEntity<History> getHistoryById(@PathVariable Long id) {
        return historyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Создать новую запись
    @PostMapping
    public ResponseEntity<History> createHistory(@RequestBody History history) {
        History saved = historyService.save(history);
        return ResponseEntity.ok(saved);
    }

    // Обновить существующую запись
    @PutMapping("/{id}")
    public ResponseEntity<History> updateHistory(@PathVariable Long id, @RequestBody History history) {
        return historyService.findById(id)
                .map( existing -> {
                    existing.setContent(history.getContent());
                    existing.setTask(history.getTask());
                    History updated = historyService.save(existing);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Удалить запись по id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        if (historyService.findById(id).isPresent()) {
            historyService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Получить историю по ID задачи
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Optional<Set<History>>> getHistoryByTaskId(@PathVariable Long taskId) {
        Optional<Set<History>> histories = historyService.findByTaskId(taskId);
        return ResponseEntity.ok(histories);
    }
}
