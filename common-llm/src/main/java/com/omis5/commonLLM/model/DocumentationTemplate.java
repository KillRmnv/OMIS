package com.omis5.commonLLM.model;

import java.util.Map;

/**
 * Модель шаблона документации.
 * Поддерживает различные форматы: mkdocs, комментарии в коде, markdown и т.д.
 */
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
    
    private Long id;
    private String name;
    private String description;
    private TemplateType type;
    private String content;  // Содержимое шаблона (может быть markdown, yaml и т.д.)
    private Map<String, String> metadata;  // Дополнительные метаданные (например, версия, автор)
    
    public DocumentationTemplate() {
    }
    
    public DocumentationTemplate(String name, String description, TemplateType type, String content) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public TemplateType getType() {
        return type;
    }
    
    public void setType(TemplateType type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
