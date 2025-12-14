package com.omis5.distributionService.services;

import com.omis5.distributionService.model.Status;
import com.omis5.distributionService.model.Task;
import com.omis5.distributionService.repositories.TaskRepository;
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
public class TaskService {

    private final TaskRepository taskRepository;

    public Task save(Task task) {
        log.info("Saving task with creatorId: {}, docsId: {}", task.getCreatorId(), task.getDocsId());
        Task saved = taskRepository.save(task);
        log.debug("Task saved with id: {}", saved.getId());
        return saved;
    }

    public Optional<Task> findById(Long id) {
        log.debug("Finding task by id: {}", id);
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            log.debug("Task found with id: {}", id);
        } else {
            log.warn("Task not found with id: {}", id);
        }
        return task;
    }

    public List<Task> findAll() {
        log.debug("Finding all tasks");
        List<Task> tasks = taskRepository.findAll();
        log.info("Found {} tasks", tasks.size());
        return tasks;
    }

    public void deleteById(Long id) {
        log.info("Deleting task with id: {}", id);
        taskRepository.deleteById(id);
        log.debug("Task deleted successfully");
    }

    public List<Task> findByStatusId(Status statusId) {
        log.debug("Finding tasks by statusId: {}", statusId.getId());
        List<Task> tasks = taskRepository.findByStatus(statusId);
        log.info("Found {} tasks with statusId: {}", tasks.size(), statusId.getId());
        return tasks;
    }
}
