package com.omis5.commonLLM;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GroqLlmClient implements LlmInterface {

    private final String apiUrl;
    private final String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqLlmClient() {
        this.apiUrl = EnvConfig.get("GROQ_API_URL");
        this.apiKey = EnvConfig.get("GROQ_API_KEY");
    }

    @Override
    public String sendChatCompletion(String modelName,
                                     List<Map<String, String>> messages,
                                     Map<String, Object> options) throws Exception {

        log.info("sendChatCompletion(model={}, messages={}, options={})",
                modelName, messages.size(), options);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        if (options != null) {
            body.putAll(options);
        }

        String json = objectMapper.writeValueAsString(body);

        log.debug("JSON request body = {}", json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.info("Sending request to Groq: {}", apiUrl);

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Groq status = {}", response.statusCode());
        log.debug("Raw body = {}", response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq returned status " + response.statusCode());
        }

        Map<String, Object> responseJson =
                objectMapper.readValue(response.body(), Map.class);

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseJson.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("Groq returned no choices");
        }

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }
}
