package com.omis5.validationService.controllers;

import com.omis5.commonLLM.providers.Providers;
import com.omis5.commonTemplate.model.DocumentationTemplate;
import com.omis5.validationService.dto.ErrorResponse;
import com.omis5.validationService.dto.ValidationRequest;
import com.omis5.validationService.dto.ValidationResponse;
import com.omis5.validationService.services.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/validation")
@Slf4j
public class LlmController {
    
    private final LlmService llmService;

    @Autowired
    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Валидирует документацию на соответствие шаблону и стандартам оформления
     * 
     * POST /api/validation/validate
     * {
     *   "documentation": "текст документации",
     *   "template": {
     *     "name": "название шаблона",
     *     "type": "MARKDOWN",
     *     "content": "содержимое шаблона"
     *   },
     *   "additionalStandards": "дополнительные стандарты (опционально)",
     *   "model": "llama-3.3-70b-versatile",
     *   "provider": "GROQ"
     * }
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateDocumentation(@RequestBody Map<String, Object> requestBody) {
        log.info("=== DOCUMENTATION VALIDATION REQUEST START ===");
        log.debug("Request body keys: {}", requestBody.keySet());
        
        try {
            // Парсим запрос
            ValidationRequest request = parseValidationRequest(requestBody);
            log.debug("Parsed ValidationRequest with template: {}", 
                    request.getTemplate() != null ? request.getTemplate().getName() : "null");
            log.debug("Documentation length: {} characters", 
                    request.getDocumentation() != null ? request.getDocumentation().length() : 0);
            
            // Получаем параметры модели
            String modelName = (String) requestBody.getOrDefault("model", "llama-3.3-70b-versatile");
            String providerStr = (String) requestBody.getOrDefault("provider", "GROQ");
            Providers provider;
            try {
                provider = Providers.valueOf(providerStr.toUpperCase());
                log.debug("Using provider: {}", provider);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid provider: {}. Using GROQ", providerStr);
                provider = Providers.GROQ;
            }
            
            // Валидируем документацию
            log.info("Starting documentation validation with model: {}, provider: {}", modelName, provider);
            ValidationResponse validationResponse = llmService.validateDocumentation(request, modelName, provider);
            log.debug("Validation completed. isValid: {}, issues count: {}", 
                    validationResponse.isValid(), 
                    validationResponse.getIssues() != null ? validationResponse.getIssues().size() : 0);
            
            log.info("=== DOCUMENTATION VALIDATION REQUEST SUCCESS ===");
            return ResponseEntity.ok(validationResponse);
            
        } catch (Exception e) {
            log.error("=== DOCUMENTATION VALIDATION REQUEST FAILED ===");
            log.error("Error in /validate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to validate documentation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработчик исключений для возврата ErrorResponse
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException caught: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    /**
     * Парсит запрос в ValidationRequest
     */
    private ValidationRequest parseValidationRequest(Map<String, Object> requestBody) {
        log.debug("Parsing validation request");
        ValidationRequest request = new ValidationRequest();
        
        // Парсим документацию
        String documentation = (String) requestBody.get("documentation");
        if (documentation == null || documentation.isEmpty()) {
            log.warn("Documentation is missing or empty in request");
            throw new IllegalArgumentException("Documentation is required");
        }
        request.setDocumentation(documentation);
        log.debug("Documentation parsed, length: {} characters", documentation.length());
        
        // Парсим шаблон
        Map<String, Object> templateMap = (Map<String, Object>) requestBody.get("template");
        if (templateMap == null) {
            log.warn("Template is missing in request");
            throw new IllegalArgumentException("Template is required");
        }
        
        DocumentationTemplate template = new DocumentationTemplate();
        template.setName((String) templateMap.get("name"));
        template.setDescription((String) templateMap.get("description"));
        if (templateMap.get("type") != null) {
            try {
                template.setType(DocumentationTemplate.TemplateType.valueOf(
                    ((String) templateMap.get("type")).toUpperCase()
                ));
            } catch (Exception e) {
                log.warn("Invalid template type, using MARKDOWN as default");
                template.setType(DocumentationTemplate.TemplateType.MARKDOWN);
            }
        }
        template.setContent((String) templateMap.get("content"));
        request.setTemplate(template);
        log.debug("Template parsed: name={}, type={}", template.getName(), template.getType());
        
        // Парсим дополнительные стандарты (опционально)
        String additionalStandards = (String) requestBody.get("additionalStandards");
        request.setAdditionalStandards(additionalStandards);
        if (additionalStandards != null) {
            log.debug("Additional standards provided, length: {} characters", additionalStandards.length());
        }
        
        log.debug("Validation request parsed successfully");
        return request;
    }
}
