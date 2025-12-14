package com.omis5.commonTemplate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Модель запроса на генерацию документации.
 * Содержит исходники (код), текстовые описания, спецификации и шаблон.
 * 
 * Примечание: Это не JPA entity, а DTO для передачи данных между сервисами.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DocumentationRequest {
    
    private DocumentationTemplate template;
    private List<CodeSource> codeSources;  // Исходный код
    private List<String> textDescriptions;  // Текстовые описания
    private List<String> specifications;  // Спецификации
    private Map<String, Object> additionalParams;  // Дополнительные параметры
    
    /**
     * Модель исходного кода
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CodeSource {
        private String fileName;
        private String language;  // java, python, javascript и т.д.
        private String content;  // Содержимое файла
    }
}
