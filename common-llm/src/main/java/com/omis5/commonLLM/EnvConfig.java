package com.omis5.commonLLM;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {

    private static final Dotenv dotenv =
            Dotenv.configure()
                    .ignoreIfMissing()   // .env может отсутствовать
                    .load();

    public static String get(String key) {
        return dotenv.get(key);
    }
}
