package com.omis5.distributionService.controllers;

import com.omis5.distributionService.model.Status;
import com.omis5.distributionService.services.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribution/status")
@RequiredArgsConstructor
@Slf4j
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    public ResponseEntity<List<Status>> getAllStatuses() {
        log.info("GET /api/distribution/status - Getting all statuses");
        List<Status> statuses = statusService.findAll();
        log.debug("Returning {} statuses", statuses.size());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Status> getStatusById(@PathVariable Long id) {
        log.info("GET /api/distribution/status/{} - Getting status by id", id);
        return statusService.findById(id)
                .map(status -> {
                    log.debug("Status found with id: {}", id);
                    return ResponseEntity.ok(status);
                })
                .orElseGet(() -> {
                    log.warn("Status not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Status> createStatus(@RequestBody Status status) {
        log.info("POST /api/distribution/status - Creating new status: {}", status.getStatus());
        Status saved = statusService.save(status);
        log.info("Status created successfully with id: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Status> updateStatus(@PathVariable Long id, @RequestBody Status status) {
        log.info("PUT /api/distribution/status/{} - Updating status", id);
        return statusService.findById(id)
                .map(existing -> {
                    existing.setStatus(status.getStatus());
                    Status updated = statusService.save(existing);
                    log.info("Status updated successfully");
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> {
                    log.warn("Status not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        log.info("DELETE /api/distribution/status/{} - Deleting status", id);
        if (statusService.findById(id).isPresent()) {
            statusService.deleteById(id);
            log.info("Status deleted successfully");
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Status not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
