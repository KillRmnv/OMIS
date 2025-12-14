package com.omis5.docGenerationService.services.parsers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фабрика для получения парсеров кода по языку
 */
@Component
@Slf4j
public class CodeParserFactory {
    
    private final Map<String, CodeParserInterface> parsers = new HashMap<>();
    
    @Autowired
    public CodeParserFactory(List<CodeParserInterface> parserList) {
        for (CodeParserInterface parser : parserList) {
            parsers.put(parser.getSupportedLanguage().toLowerCase(), parser);
            log.info("Registered code parser for language: {}", parser.getSupportedLanguage());
        }
    }
    
    /**
     * Получает парсер для указанного языка
     * @param language язык программирования
     * @return парсер или null, если не найден
     */
    public CodeParserInterface getParser(String language) {
        if (language == null) {
            log.warn("Language is null, returning null parser");
            return null;
        }
        
        CodeParserInterface parser = parsers.get(language.toLowerCase());
        if (parser == null) {
            log.warn("No parser found for language: {}", language);
        }
        return parser;
    }
    
    /**
     * Проверяет, поддерживается ли язык
     * @param language язык программирования
     * @return true, если язык поддерживается
     */
    public boolean isLanguageSupported(String language) {
        return language != null && parsers.containsKey(language.toLowerCase());
    }
}
