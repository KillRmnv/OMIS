package com.omis5.docGenerationService.controllers;

import com.omis5.docGenerationService.services.LlmService;
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

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

            String response = "";
//                    llmClient.sendChatCompletion(modelName, messages, Map.of());
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



}