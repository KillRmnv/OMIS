package com.omis5.docGenerationService.repositories;

import com.omis5.commonTemplate.model.DocumentationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с шаблонами документации
 */
@Repository
public interface DocumentationTemplateRepository extends JpaRepository<DocumentationTemplate, Long> {
    
    /**
     * Находит шаблон по имени
     */
    Optional<DocumentationTemplate> findByName(String name);
    
    /**
     * Находит все шаблоны по типу
     */
    List<DocumentationTemplate> findByType(DocumentationTemplate.TemplateType type);
    
    /**
     * Проверяет существование шаблона по имени
     */
    boolean existsByName(String name);
}
