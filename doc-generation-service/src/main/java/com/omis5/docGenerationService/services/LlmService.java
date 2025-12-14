package com.omis5.docGenerationService.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.ApplicationScope;

import com.omis5.commonLLM.LLMFactory;
import com.omis5.commonLLM.LlmInterface;
import com.omis5.commonLLM.providers.LLMProvider;
import com.omis5.commonLLM.providers.LLMAPIProviderFactory;
import com.omis5.commonLLM.providers.Providers;
import com.omis5.commonTemplate.model.DocumentationRequest;
import com.omis5.commonTemplate.model.DocumentationTemplate;

import lombok.extern.slf4j.Slf4j;

@ApplicationScope
@Service
@Slf4j
//TODO: fix prompt building.Make it loop
public class LlmService {

    
    @Value("${file-storage.service.url:http://localhost:8083}")
    private String fileStorageServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final LLMFactory llmFactory = new LLMFactory();
    private final LLMAPIProviderFactory providerFactory = new LLMAPIProviderFactory();
    
    @Autowired
    private CodeParser codeParser;
    
    // Системные промпты для генерации документации
    private static final String SYSTEM_PROMPT_DOC_GENERATION = """
            Ты — эксперт по генерации технической документации.
            Твоя задача — создавать качественную, структурированную документацию на основе предоставленного кода, описаний и спецификаций.
            
            ТРЕБОВАНИЯ:
            1. Анализируй код и выделяй ключевые функции, классы, методы
            2. Описывай параметры, возвращаемые значения, исключения
            3. Используй предоставленный шаблон для форматирования
            4. Будь точным и лаконичным
            5. Следуй стилю и формату указанного шаблона документации
            6. Включай примеры использования, если это уместно
            
            Отвечай на языке запроса пользователя.
            """;
    

    
    /**
     * Генерирует документацию на основе запроса
     */
    public String generateDocumentation(DocumentationRequest request, String modelName, Providers provider) throws Exception {
        log.info("Starting documentation generation for template: {}", 
                request.getTemplate() != null ? request.getTemplate().getName() : "null");
        log.debug("Model: {}, Provider: {}", modelName, provider);
        
        // 1. Разбиваем код на функции
        List<CodeParser.FunctionInfo> allFunctions = new ArrayList<>();
        if (request.getCodeSources() != null) {
            for (DocumentationRequest.CodeSource codeSource : request.getCodeSources()) {
                List<CodeParser.FunctionInfo> functions = codeParser.parseCodeIntoFunctions(codeSource);
                allFunctions.addAll(functions);
                log.info("Parsed {} functions from {}", functions.size(), codeSource.getFileName());
            }
        }else{
            log.info("No codeSources found");
        }
        
        // 2. Формируем промпт для LLM
        String userPrompt = buildDocumentationPrompt(request, allFunctions);
        
        // 3. Формируем сообщения для LLM
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT_DOC_GENERATION));
        messages.add(Map.of("role", "user", "content", userPrompt));
        
        // 4. Получаем LLM клиент через фабрику
        LlmInterface llmClient = llmFactory.getLLM(provider);
        
        // 5. Отправляем запрос
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.3);
        options.put("max_tokens", 4000);
        
        log.info("Sending request to LLM model: {}", modelName);
        String generatedDoc = llmClient.sendChatCompletion(modelName, messages, options);
        log.info("Documentation generated successfully, length: {} chars", generatedDoc.length());
        
        return generatedDoc;
    }
    
    /**
     * Строит промпт для генерации документации.fix it
     */
    private String buildDocumentationPrompt(DocumentationRequest request, List<CodeParser.FunctionInfo> functions) {
        StringBuilder prompt = new StringBuilder();
        
        // Шаблон
        DocumentationTemplate template = request.getTemplate();
        prompt.append("ШАБЛОН ДОКУМЕНТАЦИИ:\n");
        prompt.append("Тип: ").append(template.getType()).append("\n");
        prompt.append("Название: ").append(template.getName()).append("\n");
        if (template.getDescription() != null) {
            prompt.append("Описание: ").append(template.getDescription()).append("\n");
        }
        prompt.append("\nСодержимое шаблона:\n").append(template.getContent()).append("\n\n");
        
        // Код (разбитый по функциям)
        if (!functions.isEmpty()) {
            prompt.append("ИСХОДНЫЙ КОД (разбит по функциям):\n\n");
            for (CodeParser.FunctionInfo func : functions) {
                prompt.append("Функция: ").append(func.getName()).append("\n");
                prompt.append("```\n").append(func.getBody()).append("\n```\n\n");
            }
        }
        
        // Текстовые описания
        if (request.getTextDescriptions() != null && !request.getTextDescriptions().isEmpty()) {
            prompt.append("ТЕКСТОВЫЕ ОПИСАНИЯ:\n");
            for (String desc : request.getTextDescriptions()) {
                prompt.append("- ").append(desc).append("\n");
            }
            prompt.append("\n");
        }
        
        // Спецификации
        if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
            prompt.append("СПЕЦИФИКАЦИИ:\n");
            for (String spec : request.getSpecifications()) {
                prompt.append("- ").append(spec).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("ЗАДАЧА: Сгенерируй документацию согласно предоставленному шаблону, используя весь предоставленный контент.");
        
        return prompt.toString();
    }
    
    /**
     * Получает все доступные модели от всех провайдеров
     */
    public Map<String, Map<String, String>> getAllAvailableModels() {
        Map<String, Map<String, String>> allModels = new HashMap<>();
        
        for (Providers provider : Providers.values()) {
            try {
                LLMProvider llmProvider = providerFactory.getProvider(provider);
                Map<String, String> models = llmProvider.getAvailableModels();
                allModels.put(provider.name(), models);
                log.info("Retrieved {} models from provider {}", models.size(), provider);
            } catch (Exception e) {
                log.error("Error getting models from provider {}: {}", provider, e.getMessage());
                allModels.put(provider.name(), new HashMap<>());
            }
        }
        
        return allModels;
    }
    
    /**
     * Сохраняет сгенерированную документацию в файловый сервис (висячий запрос)
     */
    public void saveGeneratedDocumentation(String documentation, String fileName, Map<String, Object> metadata) {
        log.info("Attempting to save documentation to file storage service: {}", fileStorageServiceUrl);
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("fileName", fileName);
            requestBody.put("content", documentation);
            requestBody.put("metadata", metadata);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Висячий запрос - сервис будет реализован позже
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                fileStorageServiceUrl + "/api/files/save",
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            log.info("Documentation saved successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to save documentation to file storage service (service may not exist yet): {}", e.getMessage());
            // Не бросаем исключение, т.к. сервис может быть еще не реализован
        }
    }

}
