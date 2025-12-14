package com.omis5.docGenerationService.services.parsers;

import com.omis5.commonTemplate.model.DocumentationRequest;
import com.omis5.docGenerationService.services.CodeParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер для Java кода
 */
@Component
@Slf4j
public class JavaCodeParser implements CodeParserInterface {
    
    @Override
    public List<CodeParser.FunctionInfo> parse(DocumentationRequest.CodeSource codeSource) {
        log.debug("Parsing Java code from file: {}", codeSource.getFileName());
        String code = codeSource.getContent();
        List<CodeParser.FunctionInfo> functions = new ArrayList<>();
        
        // Паттерн для Java методов (включая модификаторы, возвращаемый тип, имя, параметры)
        Pattern pattern = Pattern.compile(
            "(?:(?:public|private|protected|static|final|abstract|synchronized)\\s+)*" +
            "(?:[\\w<>\\[\\]\\s]+\\s+)?" +  // возвращаемый тип
            "(\\w+)\\s*" +  // имя метода
            "\\([^)]*\\)\\s*" +  // параметры
            "(?:throws\\s+[^{]+)?\\s*" +  // throws clause
            "\\{",
            Pattern.MULTILINE | Pattern.DOTALL
        );
        
        Matcher matcher = pattern.matcher(code);
        
        while (matcher.find()) {
            String methodName = matcher.group(1);
            int start = matcher.start();
            
            // Находим конец метода (соответствующая закрывающая скобка)
            int end = findMatchingBrace(code, matcher.end() - 1);
            
            if (end > start) {
                String methodBody = code.substring(start, end + 1);
                functions.add(new CodeParser.FunctionInfo(methodName, methodBody, start, end + 1));
                log.debug("Found Java method: {} at position {}-{}", methodName, start, end);
            }
        }
        
        if (functions.isEmpty()) {
            log.warn("No Java methods found in file: {}. Returning full content as single function.", codeSource.getFileName());
            functions.add(new CodeParser.FunctionInfo("main", code, 0, code.length()));
        }
        
        log.info("Parsed {} Java methods from file: {}", functions.size(), codeSource.getFileName());
        return functions;
    }
    
    @Override
    public String getSupportedLanguage() {
        return "java";
    }
    
    private int findMatchingBrace(String code, int startPos) {
        int depth = 1;
        int pos = startPos + 1;
        
        while (pos < code.length() && depth > 0) {
            char c = code.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }
        
        return depth == 0 ? pos - 1 : code.length();
    }
}
