package com.omis5.commonLLM;

import com.omis5.commonLLM.providers.Providers;


public class LLMFactory {
    public LlmInterface getLLM(Providers provider) {
        switch(provider){
            case GROQ:
                return new GroqLlmClient();
            case GEMINI:
                return new GeminiLlmClient();
            default:
                throw new IllegalStateException("Unexpected value: " + provider);
        }
    }
}
