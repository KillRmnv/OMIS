package com.omis5.validationService.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omis5.commonLLM.LLMFactory;
import com.omis5.commonLLM.LlmInterface;
import com.omis5.commonLLM.providers.Providers;
import com.omis5.commonTemplate.model.DocumentationTemplate;
import com.omis5.validationService.dto.ValidationRequest;
import com.omis5.validationService.dto.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.*;

@ApplicationScope
@Service
@Slf4j
public class LlmService {

    
    private final LLMFactory llmFactory = new LLMFactory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Системный промпт для валидации документации
    private static final String SYSTEM_PROMPT_VALIDATION = """
            Ты — эксперт по валидации технической документации.
            Твоя задача — проверить документацию на соответствие шаблону и стандартам оформления.
            
            ТРЕБОВАНИЯ К АНАЛИЗУ:
            1. Проверь структуру документации (заголовки, разделы, подразделы)
            2. Проверь форматирование (markdown, списки, таблицы, код)
            3. Проверь соответствие шаблону (тип, стиль, формат)
            4. Проверь полноту информации (все ли разделы присутствуют)
            5. Проверь качество контента (ясность, точность, полнота описаний)
            6. Проверь соблюдение стандартов оформления документации
            
            ФОРМАТ ОТВЕТА (строго JSON):
            {
                "isValid": true/false,
                "complianceScore": 0-100,
                "summary": "Краткое резюме валидации",
                "issues": [
                    {
                        "type": "FORMAT|CONTENT|STRUCTURE|STANDARD",
                        "severity": "ERROR|WARNING|INFO",
                        "description": "Подробное описание проблемы",
                        "location": "Где найдена проблема (раздел, строка и т.д.)",
                        "recommendation": "Рекомендация по исправлению"
                    }
                ]
            }
            
            ВАЖНО:
            - Если документация полностью соответствует стандартам, isValid=true, issues=[]
            - Если есть проблемы, четко укажи что именно не соответствует
            - Будь конкретным в описании проблем
            - Предоставляй практические рекомендации по исправлению
            """;
    
    /**
     * Валидирует документацию на соответствие шаблону и стандартам
     */
    public ValidationResponse validateDocumentation(
            ValidationRequest request, 
            String modelName, 
            Providers provider) throws Exception {
        
        log.info("=== DOCUMENTATION VALIDATION PROCESS START ===");
        log.info("Template name: {}", request.getTemplate() != null ? request.getTemplate().getName() : "null");
        log.info("Template type: {}", request.getTemplate() != null ? request.getTemplate().getType() : "null");
        log.info("Documentation length: {} characters", request.getDocumentation() != null ? request.getDocumentation().length() : 0);
        log.info("Model: {}, Provider: {}", modelName, provider);
        
        // 1. Формируем промпт для валидации
        log.info("Step 1: Building validation prompt");
        String userPrompt = buildValidationPrompt(request);
        log.debug("Validation prompt length: {} characters", userPrompt.length());
        log.trace("Validation prompt content: {}", userPrompt);
        
        // 2. Формируем сообщения для LLM
        log.info("Step 2: Preparing messages for LLM");
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT_VALIDATION));
        messages.add(Map.of("role", "user", "content", userPrompt));
        log.debug("Prepared {} messages for LLM", messages.size());
        
        // 3. Получаем LLM клиент через фабрику
        log.info("Step 3: Getting LLM client from factory for provider: {}", provider);
        LlmInterface llmClient = llmFactory.getLLM(provider);
        log.debug("LLM client obtained successfully");
        
        // 4. Отправляем запрос
        log.info("Step 4: Sending validation request to LLM");
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.1); // Низкая температура для более детерминированного ответа
        options.put("max_tokens", 2000);
        log.debug("LLM request options: temperature={}, max_tokens={}", 
                options.get("temperature"), options.get("max_tokens"));
        
        long startTime = System.currentTimeMillis();
        log.info("Sending validation request to LLM model: {} via provider: {}", modelName, provider);
        String llmResponse = llmClient.sendChatCompletion(modelName, messages, options);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("LLM validation response received in {} ms", duration);
        log.debug("LLM raw response length: {} characters", llmResponse.length());
        log.trace("LLM raw response: {}", llmResponse);
        
        // 5. Парсим ответ от LLM
        log.info("Step 5: Parsing LLM response");
        ValidationResponse validationResponse = parseValidationResponse(llmResponse);
        log.info("Validation completed. isValid: {}, complianceScore: {}, issues count: {}", 
                validationResponse.isValid(), 
                validationResponse.getComplianceScore(),
                validationResponse.getIssues() != null ? validationResponse.getIssues().size() : 0);
        
        if (!validationResponse.isValid() && validationResponse.getIssues() != null) {
            log.warn("Documentation validation found {} issues", validationResponse.getIssues().size());
            validationResponse.getIssues().forEach(issue -> 
                log.warn("Issue [{}]: {} - {}", issue.getSeverity(), issue.getType(), issue.getDescription())
            );
        } else {
            log.info("Documentation validation passed successfully");
        }
        
        log.info("=== DOCUMENTATION VALIDATION PROCESS SUCCESS ===");
        return validationResponse;
    }
    
    /**
     * Строит промпт для валидации документации
     */
    private String buildValidationPrompt(ValidationRequest request) {
        log.debug("Building validation prompt");
        StringBuilder prompt = new StringBuilder();
        
        // Шаблон
        DocumentationTemplate template = request.getTemplate();
        log.debug("Adding template information to prompt: name={}, type={}", 
                template.getName(), template.getType());
        prompt.append("ШАБЛОН ДОКУМЕНТАЦИИ:\n");
        prompt.append("Тип: ").append(template.getType()).append("\n");
        prompt.append("Название: ").append(template.getName()).append("\n");
        if (template.getDescription() != null) {
            prompt.append("Описание: ").append(template.getDescription()).append("\n");
            log.debug("Template description length: {} characters", template.getDescription().length());
        }
        prompt.append("\nСодержимое шаблона:\n").append(template.getContent()).append("\n\n");
        log.debug("Template content length: {} characters", template.getContent().length());
        
        // Дополнительные стандарты
        if (request.getAdditionalStandards() != null && !request.getAdditionalStandards().isEmpty()) {
            log.debug("Adding additional standards to prompt");
            prompt.append("ДОПОЛНИТЕЛЬНЫЕ СТАНДАРТЫ ОФОРМЛЕНИЯ:\n");
            prompt.append(request.getAdditionalStandards()).append("\n\n");
        } else {
            log.debug("No additional standards provided");
        }
        
        // Документация для валидации
        prompt.append("ДОКУМЕНТАЦИЯ ДЛЯ ВАЛИДАЦИИ:\n");
        prompt.append("```\n").append(request.getDocumentation()).append("\n```\n\n");
        log.debug("Documentation content length: {} characters", request.getDocumentation().length());
        
        prompt.append("ЗАДАЧА: Проверь документацию на соответствие шаблону и стандартам оформления. " +
                "Укажи все найденные проблемы и дай рекомендации по их исправлению. " +
                "Ответь строго в формате JSON, указанном в системном промпте.");
        
        String finalPrompt = prompt.toString();
        log.debug("Prompt building completed. Total length: {} characters", finalPrompt.length());
        return finalPrompt;
    }
    
    /**
     * Парсит ответ от LLM в ValidationResponse
     */
    private ValidationResponse parseValidationResponse(String llmResponse) {
        log.debug("Parsing LLM response");
        try {
            // Пытаемся извлечь JSON из ответа (может быть обернут в markdown код)
            String jsonResponse = llmResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.substring(3);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            jsonResponse = jsonResponse.trim();
            
            log.trace("Extracted JSON: {}", jsonResponse);
            
            ValidationResponse response = objectMapper.readValue(jsonResponse, ValidationResponse.class);
            log.debug("Successfully parsed validation response");
            return response;
        } catch (Exception e) {
            log.error("Failed to parse LLM response as JSON: {}", e.getMessage());
            log.debug("Raw LLM response: {}", llmResponse);
            log.debug("Stack trace: ", e);
            
            // Возвращаем ответ с ошибкой парсинга
            ValidationResponse errorResponse = new ValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setComplianceScore(0);
            errorResponse.setSummary("Ошибка при парсинге ответа от LLM: " + e.getMessage());
            errorResponse.setIssues(new ArrayList<>());
            
            ValidationResponse.ValidationIssue issue = new ValidationResponse.ValidationIssue();
            issue.setType("PARSING_ERROR");
            issue.setSeverity("ERROR");
            issue.setDescription("Не удалось распарсить ответ от LLM. Возможно, ответ не в формате JSON.");
            issue.setLocation("LLM Response");
            issue.setRecommendation("Проверьте формат ответа от LLM модели.");
            errorResponse.getIssues().add(issue);
            
            return errorResponse;
        }
    }
}

