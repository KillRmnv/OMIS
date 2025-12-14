package com.omis5.docGenerationService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO для ответа на запрос генерации документации
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationGenerationResponse {
    private String documentation;
    private String model;
    private String provider;
}
