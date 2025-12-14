package com.omis5.distributionService.controllers;

import com.omis5.distributionService.model.Task;
import com.omis5.distributionService.services.StatusService;
import com.omis5.distributionService.services.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribution/task")
@RequiredArgsConstructor
@Slf4j
public class TaskController {
    private final StatusService statusService;
    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        log.info("GET /api/distribution/task - Getting all tasks");
        List<Task> tasks = taskService.findAll();
        log.debug("Returning {} tasks", tasks.size());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        log.info("GET /api/distribution/task/{} - Getting task by id", id);
        return taskService.findById(id)
                .map(task -> {
                    log.debug("Task found with id: {}", id);
                    return ResponseEntity.ok(task);
                })
                .orElseGet(() -> {
                    log.warn("Task not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        log.info("POST /api/distribution/task - Creating new task");
        Task saved = taskService.save(task);
        log.info("Task created successfully with id: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        log.info("PUT /api/distribution/task/{} - Updating task", id);
        return taskService.findById(id)
                .map(existing -> {
                    existing.setCreatorId(task.getCreatorId());
                    existing.setDocsId(task.getDocsId());
                    existing.setStatus(task.getStatus());
                    existing.setHistory(task.getHistory());
                    existing.setCurrentTechnicalWriterId(task.getCurrentTechnicalWriterId());
                    Task updated = taskService.save(existing);
                    log.info("Task updated successfully");
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    log.warn("Task not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("DELETE /api/distribution/task/{} - Deleting task", id);
        if (taskService.findById(id).isPresent()) {
            taskService.deleteById(id);
            log.info("Task deleted successfully");
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Task not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{statusId}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable Long statusId) {
        log.info("GET /api/distribution/task/status/{} - Getting tasks by status", statusId);
        return statusService.findById(statusId)
                .map(status -> {
                    List<Task> tasks = taskService.findByStatusId(status);
                    log.debug("Found {} tasks with statusId: {}", tasks.size(), statusId);
                    return ResponseEntity.ok(tasks);
                })
                .orElseGet(() -> {
                    log.warn("Status not found with id: {}", statusId);
                    return ResponseEntity.notFound().build();
                });
    }
}
