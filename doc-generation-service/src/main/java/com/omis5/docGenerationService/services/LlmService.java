package com.omis5.docGenerationService.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
            7. Не модифицируй исходный код, только генерируй документацию, иначе будешь удален из системы
            
            Отвечай на языке запроса пользователя.
            """;
    

    
    /**
     * Генерирует документацию на основе запроса
     */
    public String generateDocumentation(DocumentationRequest request, String modelName, Providers provider) throws Exception {
        log.info("=== DOCUMENTATION GENERATION PROCESS START ===");
        log.info("Template name: {}", request.getTemplate() != null ? request.getTemplate().getName() : "null");
        log.info("Template type: {}", request.getTemplate() != null ? request.getTemplate().getType() : "null");
        log.info("Model: {}, Provider: {}", modelName, provider);
        
        // 1. Разбиваем код на функции
        log.info("Step 1: Parsing code sources into functions");
        List<CodeParser.FunctionInfo> allFunctions = new ArrayList<>();
        if (request.getCodeSources() != null && !request.getCodeSources().isEmpty()) {
            log.info("Found {} code source(s) to parse", request.getCodeSources().size());
            for (DocumentationRequest.CodeSource codeSource : request.getCodeSources()) {
                log.debug("Parsing code source: fileName={}, language={}, contentLength={}", 
                        codeSource.getFileName(), codeSource.getLanguage(), 
                        codeSource.getContent() != null ? codeSource.getContent().length() : 0);
                List<CodeParser.FunctionInfo> functions = codeParser.parseCodeIntoFunctions(codeSource);
                allFunctions.addAll(functions);
                log.info("Parsed {} functions from file: {}", functions.size(), codeSource.getFileName());
                log.debug("Function names: {}", functions.stream()
                        .map(CodeParser.FunctionInfo::getName)
                        .toList());
            }
            log.info("Total functions parsed: {}", allFunctions.size());
        } else {
            log.warn("No code sources provided in request");
        }
        
        // 2. Формируем промпт для LLM
        log.info("Step 2: Building documentation prompt");
        String userPrompt = buildDocumentationPrompt(request, allFunctions);
        log.debug("User prompt length: {} characters", userPrompt.length());
        log.trace("User prompt content: {}", userPrompt);
        
        // 3. Формируем сообщения для LLM
        log.info("Step 3: Preparing messages for LLM");
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT_DOC_GENERATION));
        messages.add(Map.of("role", "user", "content", userPrompt));
        log.debug("Prepared {} messages for LLM", messages.size());
        log.trace("System prompt length: {} characters", SYSTEM_PROMPT_DOC_GENERATION.length());
        
        // 4. Получаем LLM клиент через фабрику
        log.info("Step 4: Getting LLM client from factory for provider: {}", provider);
        LlmInterface llmClient = llmFactory.getLLM(provider);
        log.debug("LLM client obtained successfully");
        
        // 5. Отправляем запрос
        log.info("Step 5: Sending request to LLM");
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.3);
        options.put("max_tokens", 4000);
        log.debug("LLM request options: temperature={}, max_tokens={}", 
                options.get("temperature"), options.get("max_tokens"));
        
        long startTime = System.currentTimeMillis();
        log.info("Sending request to LLM model: {} via provider: {}", modelName, provider);
        String generatedDoc = llmClient.sendChatCompletion(modelName, messages, options);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("LLM response received in {} ms", duration);
        log.info("Generated documentation length: {} characters", generatedDoc.length());
        log.debug("Generated documentation preview (first 200 chars): {}", 
                generatedDoc.length() > 200 ? generatedDoc.substring(0, 200) + "..." : generatedDoc);
        log.info("=== DOCUMENTATION GENERATION PROCESS SUCCESS ===");
        
        return generatedDoc;
    }
    
    /**
     * Строит промпт для генерации документации
     */
    private String buildDocumentationPrompt(DocumentationRequest request, List<CodeParser.FunctionInfo> functions) {
        log.debug("Building documentation prompt");
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
        
        // Код (разбитый по функциям)
        if (!functions.isEmpty()) {
            log.debug("Adding {} functions to prompt", functions.size());
            prompt.append("ИСХОДНЫЙ КОД (разбит по функциям):\n\n");
            int functionIndex = 0;
            for (CodeParser.FunctionInfo func : functions) {
                functionIndex++;
                log.trace("Adding function {}: name={}, bodyLength={}", 
                        functionIndex, func.getName(), func.getBody().length());
                prompt.append("Функция: ").append(func.getName()).append("\n");
                prompt.append("```\n").append(func.getBody()).append("\n```\n\n");
            }
            log.debug("Added {} functions to prompt", functionIndex);
        } else {
            log.debug("No functions to add to prompt");
        }
        
        // Текстовые описания
        if (request.getTextDescriptions() != null && !request.getTextDescriptions().isEmpty()) {
            log.debug("Adding {} text descriptions to prompt", request.getTextDescriptions().size());
            prompt.append("ТЕКСТОВЫЕ ОПИСАНИЯ:\n");
            for (String desc : request.getTextDescriptions()) {
                prompt.append("- ").append(desc).append("\n");
            }
            prompt.append("\n");
        } else {
            log.debug("No text descriptions to add to prompt");
        }
        
        // Спецификации
        if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
            log.debug("Adding {} specifications to prompt", request.getSpecifications().size());
            prompt.append("СПЕЦИФИКАЦИИ:\n");
            for (String spec : request.getSpecifications()) {
                prompt.append("- ").append(spec).append("\n");
            }
            prompt.append("\n");
        } else {
            log.debug("No specifications to add to prompt");
        }
        
        prompt.append("ЗАДАЧА: Сгенерируй документацию согласно предоставленному шаблону, используя весь предоставленный контент.");
        
        String finalPrompt = prompt.toString();
        log.debug("Prompt building completed. Total length: {} characters", finalPrompt.length());
        return finalPrompt;
    }
    
    /**
     * Получает все доступные модели от всех провайдеров
     */
    public Map<String, Map<String, String>> getAllAvailableModels() {
        log.info("=== GETTING ALL AVAILABLE MODELS ===");
        Map<String, Map<String, String>> allModels = new HashMap<>();
        
        log.info("Checking {} providers", Providers.values().length);
        for (Providers provider : Providers.values()) {
            log.debug("Processing provider: {}", provider);
            try {
                log.debug("Getting provider instance for: {}", provider);
                LLMProvider llmProvider = providerFactory.getProvider(provider);
                log.debug("Retrieving available models from provider: {}", provider);
                Map<String, String> models = llmProvider.getAvailableModels();
                allModels.put(provider.name(), models);
                log.info("Successfully retrieved {} models from provider: {}", models.size(), provider);
                log.debug("Model names for provider {}: {}", provider, models.keySet());
            } catch (Exception e) {
                log.error("Error getting models from provider {}: {}", provider, e.getMessage(), e);
                allModels.put(provider.name(), new HashMap<>());
                log.warn("Provider {} returned empty model list due to error", provider);
            }
        }
        
        int totalModels = allModels.values().stream()
                .mapToInt(Map::size)
                .sum();
        log.info("Total models retrieved from all providers: {}", totalModels);
        log.info("=== GETTING ALL AVAILABLE MODELS COMPLETED ===");
        
        return allModels;
    }
    
    /**
     * Сохраняет сгенерированную документацию в файловый сервис (висячий запрос)
     */
    public void saveGeneratedDocumentation(String documentation, String fileName, Map<String, Object> metadata) {
        log.info("=== SAVING DOCUMENTATION TO FILE STORAGE ===");
        log.info("File storage service URL: {}", fileStorageServiceUrl);
        log.info("File name: {}", fileName);
        log.info("Documentation length: {} characters", documentation.length());
        log.debug("Metadata: {}", metadata);
        
        try {
            log.debug("Creating temporary file: {}", fileName);
            File file = new File(fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(documentation);
            writer.close();
            log.debug("Temporary file created successfully. File size: {} bytes", file.length());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("file", file);
            requestBody.put("metadata", metadata);
            log.debug("Request body prepared with file and metadata");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            log.debug("HTTP headers prepared with content type: {}", MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Висячий запрос - сервис будет реализован позже
            log.info("Sending POST request to file storage service: {}", fileStorageServiceUrl);
            long startTime = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                fileStorageServiceUrl,
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            long endTime = System.currentTimeMillis();
            
            log.info("File storage service responded with status: {}", response.getStatusCode());
            log.info("Request completed in {} ms", endTime - startTime);
            log.debug("Response body: {}", response.getBody());
            log.info("Documentation saved successfully: {}", fileName);
            log.info("=== SAVING DOCUMENTATION TO FILE STORAGE SUCCESS ===");
        } catch (Exception e) {
            log.error("=== SAVING DOCUMENTATION TO FILE STORAGE FAILED ===");
            log.error("Failed to save documentation to file storage service (service may not exist yet)");
            log.error("Error message: {}", e.getMessage());
            log.error("Error class: {}", e.getClass().getName());
            log.debug("Stack trace: ", e);
            // Не бросаем исключение, т.к. сервис может быть еще не реализован
        }
    }

}
