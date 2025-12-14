package com.omis5.commonLLM.providers;

import java.util.Map;

public  class LocalModelsProvider implements LLMProvider {

    @Override
    public Map<String, String> getAvailableModels() {
        return Map.of();
    }
}
