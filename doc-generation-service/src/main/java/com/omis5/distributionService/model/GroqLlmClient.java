package com.omis5.distributionService.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class GroqLlmClient implements LlmInterface {

    @Value("${llm.groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${llm.groq.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();



    @Override
    public String sendChatCompletion(String modelName, List<Map<String, String>> messages, Map<String, Object> options) throws Exception {
        log.info("sendChatCompletion(model={}, messages={}, options={})",
                modelName, messages.size(), options);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        if (options != null && !options.isEmpty()) {
            body.putAll(options);
        }

        log.debug(" Request body JSON={}", objectMapper.writeValueAsString(body));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        log.info(" Sending request to Groq API: {}", apiUrl);
        log.debug(" Headers: {}", headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);

            log.info(" Groq API status: {}", response.getStatusCode());
            log.debug(" Groq API raw body: {}", response.getBody());

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("❌ Unexpected Groq status: {}", response.getStatusCode());
                throw new RuntimeException("Groq returned status " + response.getStatusCode());
            }

            var choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error(" Groq returned empty 'choices'");
                throw new RuntimeException("Groq returned no choices");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            log.info(" LLM content received: {}", content);

            return content;

        } catch (Exception e) {
            log.error(" Error calling Groq API: {}", e.getMessage(), e);
            throw e;
        }
    }
}