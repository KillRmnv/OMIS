package com.omis5.docGenerationService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * DTO для ответа со списком доступных моделей
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableModelsResponse {
    private Map<String, Map<String, String>> models;
}
