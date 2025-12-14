package com.omis5.docGenerationService.services.parsers;

import com.omis5.commonTemplate.model.DocumentationRequest;
import com.omis5.docGenerationService.services.CodeParser;

import java.util.List;

/**
 * Интерфейс для парсеров кода разных языков
 */
public interface CodeParserInterface {
    /**
     * Парсит код на функции/методы
     * @param codeSource исходный код
     * @return список функций
     */
    List<CodeParser.FunctionInfo> parse(DocumentationRequest.CodeSource codeSource);
    
    /**
     * Возвращает поддерживаемый язык
     * @return название языка (java, python, javascript и т.д.)
     */
    String getSupportedLanguage();
}
