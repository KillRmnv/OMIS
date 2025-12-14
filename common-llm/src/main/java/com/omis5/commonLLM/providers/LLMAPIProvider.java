package com.omis5.commonLLM.providers;

public interface LLMAPIProvider extends LLMProvider {
    String getApiKey();
    String getApiUrl();

}
