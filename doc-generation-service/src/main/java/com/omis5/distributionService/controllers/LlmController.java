package com.omis5.distributionService.controllers;

import com.omis5.distributionService.services.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/llm")
@Slf4j
public class LlmController {
    private LlmService llmService;
    @Value("${llm.groq.api-key}")
    String key;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    private static final String SYSTEM_PROMPT = """
            Ты — интеллектуальный ассистент медицинского приложения.
            Отвечай кратко, точно и профессионально.
            Если вопрос не относится к медицине или диагностике — отвечай нейтрально или откажись.
            Никогда не выдавай непроверенные диагнозы.Отвечай на языке запроса пользователя.Будет прикреплена медицинская
            карта пользователя с историей болезней, которые ты ранее диагностировал.Также будет прикреплено описание состояние 
            кожи,глаз,эмоционального состояния и т.д.
            """;
    private static final String PROMPT_TO_EDIT_CARD = """
    ТЫ — СИСТЕМА ОБНОВЛЕНИЯ МЕДИЦИНСКОЙ КАРТЫ. НЕ РАЗМЫШЛЯЙ. НЕ АНАЛИЗИРУЙ. НЕ ОБЪЯСНЯЙ.
    
    ЗАПРЕЩЕНО:
    - Писать <think>, </think>
    - Делать анализ, выводы, рассуждения
    - Добавлять "я думаю", "на основании", "возможно"
    - Использовать \n\n, лишние пробелы, переносы
    - Изменять формат
    - Добавлять что-либо до или после "Ответ:"
    
    РАЗРЕШЕНО ТОЛЬКО:
    СТРОГО ОДНА СТРОКА НА КАЖДОЕ ЗАБОЛЕВАНИЕ:
    Заболевание: вероятность;Описание.\n
    
    ПРАВИЛА:
    1. Вероятность — ЧИСЛО 0–100, ТОЛЬКО если УКАЗАНО ЯВНО в отчёте
    2. Если вероятность НЕ указана — ПРОПУСТИ запись
    3. Описание — КРАТКОЕ, ТОЛЬКО симптомы из отчёта
    4. Если заболевание НЕ упомянуто — НЕ ДОБАВЛЯЙ
    5. Если карта пустая — создавай ТОЛЬКО с явной вероятностью
    6. НИКАКИХ 100%, если не сказано "определённо"
    
    ФОРМАТ — СТРОГО:
    Ответ:
    Заболевание: 70;Жирная кожа, блеск.\n
    Заболевание: 95;Сухость, жажда.\n
    
    НАЧИНАЙ СРАЗУ С "Ответ:"
    ЕСЛИ НАРУШИШЬ — ТЫ БУДЕШЬ УДАЛЁН ИЗ СИСТЕМЫ.
    """;

    /**
     * Простой текстовый запрос.
     * POST /api/llm/chat
     * {
     * "model": "llama-3.3-70b-versatile",
     * "prompt": "Объясни простыми словами, что такое диабет 2 типа"
     * }
     */
    @PostMapping("/chat")
    public ResponseEntity<?> sendChat(@RequestBody Map<String, String> requestBody) {
        log.info("=== LLM CHAT REQUEST START ===");
        log.info("Request body: {}", requestBody);

        try {
            String modelName = requestBody.getOrDefault("model", "llama-3.3-70b-versatile");
            Integer userId = Integer.parseInt(requestBody.getOrDefault("userId", "-1"));

            if (userId == -1) {
                log.warn("userId is missing or invalid");
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }


            String prompt = requestBody.get("prompt");
            if (prompt == null || prompt.isEmpty()) {
                log.warn("Prompt is missing");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'prompt' field"));
            }

            log.info("Sending to LLM: model={}, prompt='{}'", modelName, prompt);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", prompt)
            );

            String response = llmClient.sendChatCompletion(modelName, messages, Map.of());
            log.info("LLM raw response: {}", response);

            modelName = "qwen/qwen3-32b";
            List<Map<String, String>> updateMessages = List.of(
                    Map.of("role", "system", "content", PROMPT_TO_EDIT_CARD),
                    Map.of("role", "user", "content", response)
            );


            log.info("=== LLM CHAT REQUEST SUCCESS ===");
            return ResponseEntity.ok(Map.of("response", response));

        } catch (Exception e) {
            log.error("Error in /chat: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Метод для отправки фото и текста.
     * Принимает multipart/form-data с файлом и текстом.
     * <p>
     * Пример запроса:
     * POST /api/llm/analyze
     * Content-Type: multipart/form-data
     * <p>
     * Параметры:
     * - file: (изображение)
     * - prompt: "Опиши состояние кожи на фото"
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeImage(@RequestParam("file") MultipartFile file) {
        log.info("=== ANALYZE IMAGE REQUEST START (Base64) ===");
        log.info("groq:" + key);
        log.info("Received file: name={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            if (file.isEmpty()) {
                log.warn("Uploaded file is empty");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            // 1. Чтение файла и кодирование в Base64
            byte[] fileBytes = file.getBytes();
            String base64EncodedImage = Base64.getEncoder().encodeToString(fileBytes);

            // Используем предоставленный MIME-тип, если он надежен.
            // Если нужен более надежный метод: String mimeType = new Tika().detect(fileBytes);
            String mimeType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

            // Формируем полный Data URI: data:<mime-type>;base64,<base64-string>
            String base64DataUri = String.format("data:%s;base64,%s", mimeType, base64EncodedImage);

            log.info("Image encoded to Base64 (MIME: {}). Size of Base64 string: {} chars",
                    mimeType, base64DataUri.length());

            // 2. Подготовка запроса с Base64
            log.info("Preparing request to Groq API for image analysis (Base64)...");
            // Теперь передаем Base64 Data URI вместо imageUrl
            HttpEntity<?> requestEntity = llmService.formPostForAnalyzeImage(key, base64DataUri);

            log.debug("Groq request body (Base64 structure): {}", requestEntity.getBody());

            log.info("Sending image analysis request to Groq: https://api.groq.com/openai/v1/chat/completions");
            ResponseEntity<Map> groqResponse = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    requestEntity,
                    Map.class
            );

            log.info("Groq API responded with status: {}", groqResponse.getStatusCode());
            Map<String, Object> body = groqResponse.getBody();

            // ... (Остальная логика обработки ответа Groq остается неизменной) ...

            if (body == null) {
                log.error("Groq response body is null");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Empty response from Groq"));
            }

            log.debug("Full Groq response body: {}", body);

            String firstWord = Optional.ofNullable(body)
                    .map(b -> (List<?>) b.get("choices"))
                    .filter(list -> !list.isEmpty())
                    .map(list -> (Map<String, Object>) list.get(0))
                    .map(choice -> (Map<String, Object>) choice.get("message"))
                    .map(msg -> (String) msg.get("content"))
                    .map(str -> {
                        log.debug("Raw LLM content: {}", str);
                        return str.trim().split("\\.", 2)[0];
                    })
                    .orElse("");

            log.info("Extracted first word from LLM response: '{}'", firstWord);

            boolean faceDetected = "Да".equalsIgnoreCase(firstWord);
            log.info("Face detected: {}", faceDetected ? "YES" : "NO");

            body.put("faceDetected", faceDetected);

            log.info("=== ANALYZE IMAGE REQUEST SUCCESS (Base64) ===");
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("Error in /analyze (Base64): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * 🎙 Метод для отправки аудиофайла и получения транскрипции.
     * Принимает multipart/form-data с аудиофайлом.
     * <p>
     * Пример запроса:
     * POST /api/llm/transcribe
     * Content-Type: multipart/form-data
     * <p>
     * Параметры:
     * - file: (аудиофайл)
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribeAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            var map = llmService.formPostForTranscribeAudio(key, file);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(map.get("url").toString(), map.get("request"), Map.class);

            return ResponseEntity.ok(Map.of(
                    "fileName", file.getOriginalFilename(),
                    "transcription", response.getBody()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

}