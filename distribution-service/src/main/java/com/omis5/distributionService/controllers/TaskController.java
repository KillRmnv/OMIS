package com.omis5.distributionService.controllers;

import com.omis5.distributionService.model.Task;
import com.omis5.distributionService.services.StatusService;
import com.omis5.distributionService.services.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribution/task")
public class TaskController {
    private final StatusService statusService;
    private final TaskService taskService;

    public TaskController(StatusService statusService, TaskService taskService) {
        this.statusService = statusService;
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.findAll();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return taskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task saved = taskService.save(task);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        return taskService.findById(id)
                .map(existing -> {
                    existing.setCreatorId(task.getCreatorId());
                    existing.setDocsId(task.getDocsId());
                    existing.setStatus(task.getStatus());
                    existing.setHistory(task.getHistory());
                    existing.setCurrentTechnicalWriterId(task.getCurrentTechnicalWriterId());
                    Task updated = taskService.save(existing);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (taskService.findById(id).isPresent()) {
            taskService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{statusId}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable Long statusId) {
        return statusService.findById(statusId).map(obj -> taskService.findByStatusId(obj)).
                map(obj -> ResponseEntity.ok(obj)).
                orElse(ResponseEntity.notFound().build());
    }
}
