package com.omis5.docGenerationService.controllers;

import com.omis5.commonLLM.model.DocumentationRequest;
import com.omis5.commonLLM.providers.Providers;
import com.omis5.docGenerationService.services.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@Slf4j
public class LlmController {
    
    private final LlmService llmService;

    @Autowired
    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }


    /**
     * Генерирует документацию на основе запроса
     */
    @PostMapping("/generate-documentation")
    public ResponseEntity<?> generateDocumentation(@RequestBody Map<String, Object> requestBody) {
        log.info("=== DOCUMENTATION GENERATION REQUEST START ===");
        
        try {
            // Парсим запрос
            DocumentationRequest request = parseDocumentationRequest(requestBody);
            
            // Получаем параметры модели
            String modelName = (String) requestBody.getOrDefault("model", "llama-3.3-70b-versatile");
            String providerStr = (String) requestBody.getOrDefault("provider", "GROQ");
            Providers provider;
            try {
                provider = Providers.valueOf(providerStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid provider: {}. Using GROQ", providerStr);
                provider = Providers.GROQ;
            }
            
            // Генерируем документацию
            String generatedDoc = llmService.generateDocumentation(request, modelName, provider);
            
            // Опционально сохраняем в файловый сервис
            Boolean saveToStorage = (Boolean) requestBody.getOrDefault("saveToStorage", false);
            if (saveToStorage != null && saveToStorage) {
                String fileName = (String) requestBody.getOrDefault("fileName", "generated_documentation.md");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) requestBody.getOrDefault("metadata", new HashMap<>());
                llmService.saveGeneratedDocumentation(generatedDoc, fileName, metadata);
            }
            
            log.info("=== DOCUMENTATION GENERATION SUCCESS ===");
            Map<String, Object> response = new HashMap<>();
            response.put("documentation", generatedDoc);
            response.put("model", modelName);
            response.put("provider", provider.name());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in /generate-documentation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получает все доступные модели от всех провайдеров
     */
    @GetMapping("/available-models")
    public ResponseEntity<?> getAvailableModels() {
        log.info("=== GET AVAILABLE MODELS REQUEST ===");
        
        try {
            Map<String, Map<String, String>> allModels = llmService.getAllAvailableModels();
            
            log.info("=== GET AVAILABLE MODELS SUCCESS ===");
            Map<String, Object> response = new HashMap<>();
            response.put("models", allModels);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in /available-models: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }
    
    /**
     * Парсит запрос в DocumentationRequest
     */
    private DocumentationRequest parseDocumentationRequest(Map<String, Object> requestBody) {
        DocumentationRequest request = new DocumentationRequest();
        
        // Парсим шаблон
        Map<String, Object> templateMap = (Map<String, Object>) requestBody.get("template");
        if (templateMap != null) {
            com.omis5.commonLLM.model.DocumentationTemplate template = new com.omis5.commonLLM.model.DocumentationTemplate();
            template.setName((String) templateMap.get("name"));
            template.setDescription((String) templateMap.get("description"));
            if (templateMap.get("type") != null) {
                try {
                    template.setType(com.omis5.commonLLM.model.DocumentationTemplate.TemplateType.valueOf(
                        ((String) templateMap.get("type")).toUpperCase()
                    ));
                } catch (Exception e) {
                    template.setType(com.omis5.commonLLM.model.DocumentationTemplate.TemplateType.MARKDOWN);
                }
            }
            template.setContent((String) templateMap.get("content"));
            request.setTemplate(template);
        }
        
        // Парсим исходный код
        List<Map<String, Object>> codeSourcesList = (List<Map<String, Object>>) requestBody.get("codeSources");
        if (codeSourcesList != null) {
            List<DocumentationRequest.CodeSource> codeSources = codeSourcesList.stream()
                .map(map -> new DocumentationRequest.CodeSource(
                    (String) map.get("fileName"),
                    (String) map.get("language"),
                    (String) map.get("content")
                ))
                .toList();
            request.setCodeSources(codeSources);
        }
        
        // Парсим текстовые описания
        List<String> textDescriptions = (List<String>) requestBody.get("textDescriptions");
        request.setTextDescriptions(textDescriptions);
        
        // Парсим спецификации
        List<String> specifications = (List<String>) requestBody.get("specifications");
        request.setSpecifications(specifications);
        
        return request;
    }

}