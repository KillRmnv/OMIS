package com.omis5.commonLLM.providers;

public class LLMProviderFactory {
    public LLMProvider getProvider(Providers providerName) {
        switch (providerName) {
            case GROQ:
                return new GroqLLMProvider();
            case GEMINI:
                return new GeminiLLMProvider();
        }
        return new GroqLLMProvider();
    }
}
