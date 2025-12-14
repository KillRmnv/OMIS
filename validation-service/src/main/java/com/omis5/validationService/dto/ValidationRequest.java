package com.omis5.validationService.dto;

import com.omis5.commonTemplate.model.DocumentationTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO для запроса валидации документации
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequest {
    /**
     * Документация для валидации
     */
    private String documentation;
    
    /**
     * Шаблон, с которым нужно сравнить документацию
     */
    private DocumentationTemplate template;
    
    /**
     * Дополнительные стандарты оформления (опционально)
     */
    private String additionalStandards;
}
