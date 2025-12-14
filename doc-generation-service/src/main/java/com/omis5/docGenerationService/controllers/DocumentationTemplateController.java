package com.omis5.docGenerationService.controllers;

import com.omis5.commonTemplate.model.DocumentationTemplate;
import com.omis5.docGenerationService.dto.ErrorResponse;
import com.omis5.docGenerationService.services.DocumentationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для CRUD операций с шаблонами документации
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class DocumentationTemplateController {
    
    private final DocumentationTemplateService templateService;
    
    /**
     * Получает все шаблоны
     */
    @GetMapping
    public ResponseEntity<List<DocumentationTemplate>> getAllTemplates() {
        log.info("GET /api/templates - Getting all templates");
        List<DocumentationTemplate> templates = templateService.findAll();
        log.debug("Returning {} templates", templates.size());
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Получает шаблон по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentationTemplate> getTemplateById(@PathVariable Long id) {
        log.info("GET /api/templates/{} - Getting template by id", id);
        return templateService.findById(id)
            .map(template -> {
                log.debug("Template found: {}", template.getName());
                return ResponseEntity.ok(template);
            })
            .orElseGet(() -> {
                log.warn("Template not found with id: {}", id);
                return ResponseEntity.notFound().build();
            });
    }
    
    /**
     * Получает шаблоны по типу
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<DocumentationTemplate>> getTemplatesByType(
            @PathVariable DocumentationTemplate.TemplateType type) {
        log.info("GET /api/templates/type/{} - Getting templates by type", type);
        List<DocumentationTemplate> templates = templateService.findByType(type);
        log.debug("Found {} templates of type: {}", templates.size(), type);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Создает новый шаблон
     */
    @PostMapping
    public ResponseEntity<DocumentationTemplate> createTemplate(@RequestBody DocumentationTemplate template) {
        log.info("POST /api/templates - Creating new template: {}", template.getName());
        
        if (templateService.existsByName(template.getName())) {
            log.warn("Template with name '{}' already exists", template.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        DocumentationTemplate saved = templateService.save(template);
        log.info("Template created successfully with id: {}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    /**
     * Обновляет шаблон
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentationTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody DocumentationTemplate template) {
        log.info("PUT /api/templates/{} - Updating template", id);
        
        try {
            DocumentationTemplate updated = templateService.update(id, template);
            log.info("Template updated successfully");
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating template: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Удаляет шаблон
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        log.info("DELETE /api/templates/{} - Deleting template", id);
        
        if (templateService.findById(id).isPresent()) {
            templateService.deleteById(id);
            log.info("Template deleted successfully");
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Template not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Обработчик исключений
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException caught: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(e.getMessage()));
    }
}
