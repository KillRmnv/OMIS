package com.omis5.commonTemplate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Модель шаблона документации.
 * Поддерживает различные форматы: mkdocs, комментарии в коде, markdown и т.д.
 */
@Entity
@Table(name = "documentation_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DocumentationTemplate {
    
    /**
     * Тип шаблона документации
     */
    public enum TemplateType {
        MKDOCS,           // MkDocs формат
        CODE_COMMENTS,    // Комментарии в коде (JavaDoc, JSDoc и т.д.)
        MARKDOWN,         // Обычный Markdown
        REST_API,         // REST API документация (OpenAPI/Swagger)
        CUSTOM            // Пользовательский формат
    }
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TemplateType type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // Содержимое шаблона (может быть markdown, yaml и т.д.)
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = MapToStringConverter.class)
    private Map<String, String> metadata;  // Дополнительные метаданные (например, версия, автор)
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
