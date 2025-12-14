package com.omis5.docGenerationService.services;

import com.omis5.commonTemplate.model.DocumentationTemplate;
import com.omis5.docGenerationService.repositories.DocumentationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для CRUD операций с шаблонами документации
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentationTemplateService {
    
    private final DocumentationTemplateRepository templateRepository;
    
    /**
     * Сохраняет шаблон
     */
    public DocumentationTemplate save(DocumentationTemplate template) {
        log.info("Saving documentation template: {}", template.getName());
        DocumentationTemplate saved = templateRepository.save(template);
        log.debug("Template saved with id: {}", saved.getId());
        return saved;
    }
    
    /**
     * Находит шаблон по ID
     */
    @Transactional(readOnly = true)
    public Optional<DocumentationTemplate> findById(Long id) {
        log.debug("Finding template by id: {}", id);
        return templateRepository.findById(id);
    }
    
    /**
     * Находит шаблон по имени
     */
    @Transactional(readOnly = true)
    public Optional<DocumentationTemplate> findByName(String name) {
        log.debug("Finding template by name: {}", name);
        return templateRepository.findByName(name);
    }
    
    /**
     * Находит все шаблоны
     */
    @Transactional(readOnly = true)
    public List<DocumentationTemplate> findAll() {
        log.debug("Finding all templates");
        List<DocumentationTemplate> templates = templateRepository.findAll();
        log.info("Found {} templates", templates.size());
        return templates;
    }
    
    /**
     * Находит все шаблоны по типу
     */
    @Transactional(readOnly = true)
    public List<DocumentationTemplate> findByType(DocumentationTemplate.TemplateType type) {
        log.debug("Finding templates by type: {}", type);
        List<DocumentationTemplate> templates = templateRepository.findByType(type);
        log.info("Found {} templates of type: {}", templates.size(), type);
        return templates;
    }
    
    /**
     * Удаляет шаблон по ID
     */
    public void deleteById(Long id) {
        log.info("Deleting template with id: {}", id);
        templateRepository.deleteById(id);
        log.debug("Template deleted successfully");
    }
    
    /**
     * Удаляет шаблон
     */
    public void delete(DocumentationTemplate template) {
        log.info("Deleting template: {}", template.getName());
        templateRepository.delete(template);
        log.debug("Template deleted successfully");
    }
    
    /**
     * Проверяет существование шаблона по имени
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        log.debug("Checking if template exists by name: {}", name);
        return templateRepository.existsByName(name);
    }
    
    /**
     * Обновляет шаблон
     */
    public DocumentationTemplate update(Long id, DocumentationTemplate updatedTemplate) {
        log.info("Updating template with id: {}", id);
        return templateRepository.findById(id)
            .map(existing -> {
                existing.setName(updatedTemplate.getName());
                existing.setDescription(updatedTemplate.getDescription());
                existing.setType(updatedTemplate.getType());
                existing.setContent(updatedTemplate.getContent());
                existing.setMetadata(updatedTemplate.getMetadata());
                DocumentationTemplate saved = templateRepository.save(existing);
                log.debug("Template updated successfully with id: {}", saved.getId());
                return saved;
            })
            .orElseThrow(() -> {
                log.error("Template not found with id: {}", id);
                return new RuntimeException("Template not found with id: " + id);
            });
    }
}
