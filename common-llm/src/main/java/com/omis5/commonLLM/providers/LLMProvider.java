package com.omis5.commonLLM.providers;

import java.util.Map;

public interface LLMProvider {
    Map<String,String> getAvailableModels();

}
