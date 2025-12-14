package com.omis5.commonLLM.model;

import java.util.List;
import java.util.Map;

/**
 * Модель запроса на генерацию документации.
 * Содержит исходники (код), текстовые описания, спецификации и шаблон.
 */
public class DocumentationRequest {
    
    private DocumentationTemplate template;
    private List<CodeSource> codeSources;  // Исходный код
    private List<String> textDescriptions;  // Текстовые описания
    private List<String> specifications;  // Спецификации
    private Map<String, Object> additionalParams;  // Дополнительные параметры
    
    public DocumentationRequest() {
    }
    
    public DocumentationRequest(DocumentationTemplate template, 
                                List<CodeSource> codeSources,
                                List<String> textDescriptions,
                                List<String> specifications) {
        this.template = template;
        this.codeSources = codeSources;
        this.textDescriptions = textDescriptions;
        this.specifications = specifications;
    }
    
    // Getters and Setters
    public DocumentationTemplate getTemplate() {
        return template;
    }
    
    public void setTemplate(DocumentationTemplate template) {
        this.template = template;
    }
    
    public List<CodeSource> getCodeSources() {
        return codeSources;
    }
    
    public void setCodeSources(List<CodeSource> codeSources) {
        this.codeSources = codeSources;
    }
    
    public List<String> getTextDescriptions() {
        return textDescriptions;
    }
    
    public void setTextDescriptions(List<String> textDescriptions) {
        this.textDescriptions = textDescriptions;
    }
    
    public List<String> getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(List<String> specifications) {
        this.specifications = specifications;
    }
    
    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }
    
    public void setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
    }
    
    /**
     * Модель исходного кода
     */
    public static class CodeSource {
        private String fileName;
        private String language;  // java, python, javascript и т.д.
        private String content;  // Содержимое файла
        
        public CodeSource() {
        }
        
        public CodeSource(String fileName, String language, String content) {
            this.fileName = fileName;
            this.language = language;
            this.content = content;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
