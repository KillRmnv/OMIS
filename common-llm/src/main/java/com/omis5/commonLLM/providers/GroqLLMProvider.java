package com.omis5.commonLLM.providers;

import com.omis5.commonLLM.EnvConfig;

import java.util.Map;

public class GroqLLMProvider implements LLMAPIProvider {

    private final String apiKey = EnvConfig.get("GROQ_API_KEY");
    private final String apiUrl = EnvConfig.get("GROQ_API_URL");

    @Override
    public Map<String, String> getAvailableModels() {
        String curl="curl -X GET \"https://api.groq.com/openai/v1/models\" \\\n" +
                "     -H \"Authorization: Bearer"+ apiKey+"\" \\\n" +
                "     -H \"Content-Type: application/json\"";

        return Map.of();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }
}
