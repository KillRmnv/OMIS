package com.omis5.docGenerationService.services;

import com.omis5.commonTemplate.model.DocumentationRequest;
import com.omis5.docGenerationService.services.parsers.CodeParserFactory;
import com.omis5.docGenerationService.services.parsers.CodeParserInterface;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для разбиения кода на функции/методы
 * Использует фабрику для получения парсеров по языкам
 */
@Component
@Slf4j
public class CodeParser {
    
    private final CodeParserFactory parserFactory;
    
    @Autowired
    public CodeParser(CodeParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }
    
    /**
     * Разбивает код на функции/методы в зависимости от языка программирования
     */
    public List<FunctionInfo> parseCodeIntoFunctions(DocumentationRequest.CodeSource codeSource) {
        log.info("Parsing code into functions for file: {}, language: {}", 
                codeSource.getFileName(), codeSource.getLanguage());
        
        String language = codeSource.getLanguage();
        CodeParserInterface parser = parserFactory.getParser(language);
        
        if (parser != null) {
            log.debug("Using parser for language: {}", language);
            return parser.parse(codeSource);
        }
        
        // Если парсер не найден, проверяем алиасы (TypeScript использует JavaScript парсер)
        if ("typescript".equalsIgnoreCase(language)) {
            log.debug("TypeScript detected, using JavaScript parser");
            parser = parserFactory.getParser("javascript");
            if (parser != null) {
                return parser.parse(codeSource);
            }
        }
        
        // Если парсер все еще не найден, возвращаем весь код как одну функцию
        log.warn("No parser found for language: {}. Returning full content as single function.", language);
        return List.of(new FunctionInfo("main", codeSource.getContent(), 0, codeSource.getContent().length()));
    }
    
    /**
     * Информация о функции
     */
    @Data
    public static class FunctionInfo {
        private String name;
        private String body;
        private int startPos;
        private int endPos;
        
        public FunctionInfo(String name, String body, int startPos, int endPos) {
            this.name = name;
            this.body = body;
            this.startPos = startPos;
            this.endPos = endPos;
        }

    }
}
