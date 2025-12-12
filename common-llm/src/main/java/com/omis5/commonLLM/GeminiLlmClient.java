package com.omis5.commonLLM;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.omis5.commonLLM.EnvConfig;

import java.util.List;
import java.util.Map;

public class GeminiLlmClient implements LlmInterface {

    private final Client client;

    public GeminiLlmClient() {
        String apiKey = EnvConfig.get("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty())
            throw new RuntimeException("Missing GEMINI_API_KEY in .env");

        client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String sendChatCompletion(String modelName,
                                     List<Map<String, String>> messages,
                                     Map<String, Object> options) throws Exception {

        StringBuilder userText = new StringBuilder();

        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");

            if ("user".equals(role)) {
                userText.append(content).append("\n");
            }
        }

        GenerateContentResponse response =
                client.models.generateContent(
                        modelName,
                        userText.toString(),
                        null
                );


        return response.text();
    }

}
