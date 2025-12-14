package com.omis5.validationService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO для ответа валидации документации
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {
    /**
     * Соответствует ли документация стандарту и шаблону
     */
    private boolean isValid;
    
    /**
     * Общая оценка соответствия (0-100)
     */
    private int complianceScore;
    
    /**
     * Список найденных проблем/несоответствий
     */
    private List<ValidationIssue> issues;
    
    /**
     * Общее резюме валидации
     */
    private String summary;
    
    /**
     * Информация о проблеме валидации
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        /**
         * Тип проблемы (FORMAT, CONTENT, STRUCTURE, STANDARD и т.д.)
         */
        private String type;
        
        /**
         * Уровень серьезности (ERROR, WARNING, INFO)
         */
        private String severity;
        
        /**
         * Описание проблемы
         */
        private String description;
        
        /**
         * Местоположение проблемы (если применимо)
         */
        private String location;
        
        /**
         * Рекомендация по исправлению
         */
        private String recommendation;
    }
}
