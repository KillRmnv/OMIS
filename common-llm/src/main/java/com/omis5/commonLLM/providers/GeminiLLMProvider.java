package com.omis5.commonLLM.providers;

import com.omis5.commonLLM.EnvConfig;
import java.util.Map;

public class GeminiLLMProvider implements LLMProvider {

    private final String apiKey = EnvConfig.get("GEMINI_API_KEY");
    private final String apiUrl = EnvConfig.get("GEMINI_API_URL");

    @Override
    public Map<String, String> getAvailableModels() {
        // TODO: запрос к Google Gemini API
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
