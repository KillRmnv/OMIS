package com.omis5.docGenerationService.services;

import com.omis5.commonLLM.LLMFactory;
import com.omis5.commonLLM.LlmInterface;
import com.omis5.commonLLM.model.DocumentationRequest;
import com.omis5.commonLLM.model.DocumentationTemplate;
import com.omis5.commonLLM.providers.LLMProvider;
import com.omis5.commonLLM.providers.LLMProviderFactory;
import com.omis5.commonLLM.providers.Providers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScope
@Service
@Slf4j
public class LlmService {
    @Value("${upload.api.key}")
    private String keyUpload;
    
    @Value("${file-storage.service.url:http://localhost:8083}")
    private String fileStorageServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final LLMFactory llmFactory = new LLMFactory();
    private final LLMProviderFactory providerFactory = new LLMProviderFactory();
    
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
    
    private static final String SYSTEM_PROMPT_FUNCTION_ANALYSIS = """
            Ты — анализатор кода. Твоя задача — проанализировать функцию/метод и предоставить структурированное описание.
            
            Для каждой функции предоставь:
            - Назначение функции
            - Параметры (тип, имя, описание)
            - Возвращаемое значение
            - Возможные исключения
            - Примеры использования (если применимо)
            
            Формат ответа должен быть структурированным и готовым для включения в документацию.
            """;
    public Map<String,Object> formPostForTranscribeAudio(String key, MultipartFile file) throws IOException {
        String url = "https://api.groq.com/openai/v1/audio/transcriptions";
        String modelName = "whisper-large-v3-turbo";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(key);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
        body.add("model", modelName);
        body.add("temperature", "0");
        body.add("response_format", "verbose_json");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String,Object> result=new HashMap<>();
        result.put("url", url);
        result.put("request", request);
        return result;
    }
    public String uploadImage(MultipartFile file) throws IOException, InterruptedException {
        log.info("=== IMAGE UPLOAD START ===");
        log.info("Original file: name={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // ШАГ 1: Определяем реальный тип файла
            byte[] bytes = file.getBytes();
            String realContentType = detectRealContentType(bytes);
            log.info("Detected real content type: {}", realContentType);

            // ШАГ 2: Конвертируем AVIF → JPEG, если нужно
            if ("image/avif".equals(realContentType)) {
                log.info("AVIF detected — converting to JPEG...");
                file = convertAvifToJpeg(file);
                log.info("AVIF → JPEG conversion completed. New size: {} bytes", file.getSize());
            }

            // ШАГ 3: Формируем multipart/form-data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("key", keyUpload);
            body.add("name", file.getOriginalFilename());
            body.add("expiration", "60");

            String contentType = "image/jpeg";
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.parseMediaType(contentType));

            HttpEntity<Resource> imagePart = new HttpEntity<>(file.getResource(), partHeaders);
            body.add("image", imagePart);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            log.debug("Multipart request body prepared:");
            log.debug("  - key: {}", keyUpload.substring(0, Math.min(8, keyUpload.length())) + "...");
            log.debug("  - name: {}", file.getOriginalFilename());
            log.debug("  - expiration: 60");
            log.debug("  - image: {} bytes, type: {}", file.getSize(), contentType);

            // ШАГ 4: Отправляем на imgbb
            log.info("Uploading image to imgbb.com...");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.imgbb.com/1/upload", request, Map.class
            );

            log.info("imgbb API responded with status: {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("imgbb returned error status or empty body: {}", response.getStatusCode());
                throw new RuntimeException("Failed to upload image to imgbb: " + response.getStatusCode());
            }

            Map<String, Object> responseBody = response.getBody();
            log.debug("Full imgbb response: {}", responseBody);

            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            if (data == null || data.get("url") == null) {
                log.error("imgbb response missing 'data.url': {}", responseBody);
                throw new RuntimeException("Invalid response from imgbb: missing image URL");
            }

            String imageUrl = data.get("url").toString();
            log.info("Image uploaded successfully! URL: {}", imageUrl);
            log.info("=== IMAGE UPLOAD SUCCESS ===");

            return imageUrl;

        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String detectRealContentType(byte[] bytes) {
        if (bytes.length < 12) return "application/octet-stream";

        // AVIF: bytes[4..7] = 'ftyp', bytes[8..11] = 'avif'
        if (bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p' &&
                bytes[8] == 'a' && bytes[9] == 'v' && bytes[10] == 'i' && bytes[11] == 'f') {
            return "image/avif";
        }

        // JPEG: FF D8
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }

        // PNG: 89 50 4E 47
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }

        return "application/octet-stream";
    }

    private MultipartFile convertAvifToJpeg(MultipartFile original) throws IOException, InterruptedException {
        File tempInput = File.createTempFile("avif_", ".avif");
        Path currentDir = Path.of("").toAbsolutePath();
        File tempOutput = new File(currentDir+"/chat-service/src/main/resources/converted"+original.getOriginalFilename());
        tempOutput.createNewFile();
        original.transferTo(tempInput);

        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", tempInput.getAbsolutePath(),
                "-y", tempOutput.getAbsolutePath());
        Process process = pb.start();
        process.waitFor();

        MultipartFile converted = new MockMultipartFile("file", tempOutput.getName(),
                "image/jpeg", Files.readAllBytes(tempOutput.toPath()));
        File file=new File(original.getOriginalFilename());
        file.delete();
        tempInput.delete();
        original=converted;
        return original;
    }
    // LlmService.java (или где определен этот метод)
    public HttpEntity<Map<String, Object>> formPostForAnalyzeImage(String key, String base64DataUri) { // Изменили имя параметра

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "meta-llama/llama-4-maverick-17b-128e-instruct");
        requestBody.put("max_completion_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "Есть ли человеческое лицо на изображении? Если есть, то опиши состояние кожи и глаз. Формат ответа: Да/Нет. Далее подробное описание.");
        content.add(textContent);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");

        Map<String, Object> imageUrlMap = new HashMap<>();
        // *** Ключевое изменение: теперь здесь Base64 Data URI ***
        imageUrlMap.put("url", base64DataUri);

        imageContent.put("image_url", imageUrlMap);

        content.add(imageContent);

        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(key);
        return new HttpEntity<>(requestBody, headers);
    }
    
    /**
     * Генерирует документацию на основе запроса
     */
    public String generateDocumentation(DocumentationRequest request, String modelName, Providers provider) throws Exception {
        log.info("Starting documentation generation for template: {}", request.getTemplate().getName());
        
        // 1. Разбиваем код на функции
        List<CodeParser.FunctionInfo> allFunctions = new ArrayList<>();
        if (request.getCodeSources() != null) {
            for (DocumentationRequest.CodeSource codeSource : request.getCodeSources()) {
                List<CodeParser.FunctionInfo> functions = codeParser.parseCodeIntoFunctions(codeSource);
                allFunctions.addAll(functions);
                log.info("Parsed {} functions from {}", functions.size(), codeSource.getFileName());
            }
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
     * Строит промпт для генерации документации
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
