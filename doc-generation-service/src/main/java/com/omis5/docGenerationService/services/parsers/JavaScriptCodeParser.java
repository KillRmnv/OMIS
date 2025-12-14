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
 * Парсер для JavaScript/TypeScript кода
 */
@Component
@Slf4j
public class JavaScriptCodeParser implements CodeParserInterface {
    
    @Override
    public List<CodeParser.FunctionInfo> parse(DocumentationRequest.CodeSource codeSource) {
        log.debug("Parsing JavaScript/TypeScript code from file: {}", codeSource.getFileName());
        String code = codeSource.getContent();
        List<CodeParser.FunctionInfo> functions = new ArrayList<>();
        
        // Паттерн для JS функций (function, arrow functions, methods)
        Pattern pattern = Pattern.compile(
            "(?:function\\s+(\\w+)|(\\w+)\\s*[:=]\\s*(?:function|\\()|(?:async\\s+)?(\\w+)\\s*=\\s*(?:async\\s*)?\\([^)]*\\)\\s*=>)" +
            "\\s*\\([^)]*\\)\\s*",
            Pattern.MULTILINE
        );
        
        Matcher matcher = pattern.matcher(code);
        
        while (matcher.find()) {
            String functionName = matcher.group(1) != null ? matcher.group(1) :
                                 matcher.group(2) != null ? matcher.group(2) :
                                 matcher.group(4) != null ? matcher.group(4) : "anonymous";
            
            int start = matcher.start();
            int end = findMatchingBrace(code, matcher.end() - 1);
            
            if (end > start) {
                String functionBody = code.substring(start, end + 1);
                functions.add(new CodeParser.FunctionInfo(functionName, functionBody, start, end + 1));
                log.debug("Found JavaScript function: {} at position {}-{}", functionName, start, end);
            }
        }
        
        if (functions.isEmpty()) {
            log.warn("No JavaScript functions found in file: {}. Returning full content as single function.", codeSource.getFileName());
            functions.add(new CodeParser.FunctionInfo("main", code, 0, code.length()));
        }
        
        log.info("Parsed {} JavaScript functions from file: {}", functions.size(), codeSource.getFileName());
        return functions;
    }
    
    @Override
    public String getSupportedLanguage() {
        return "javascript";
    }
    
    /**
     * Также поддерживает TypeScript
     */
    public String getSupportedLanguageAlias() {
        return "typescript";
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
