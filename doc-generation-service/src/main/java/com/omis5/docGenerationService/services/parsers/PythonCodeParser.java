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
 * Парсер для Python кода
 */
@Component
@Slf4j
public class PythonCodeParser implements CodeParserInterface {
    
    @Override
    public List<CodeParser.FunctionInfo> parse(DocumentationRequest.CodeSource codeSource) {
        log.debug("Parsing Python code from file: {}", codeSource.getFileName());
        String code = codeSource.getContent();
        List<CodeParser.FunctionInfo> functions = new ArrayList<>();
        
        // Паттерн для Python функций (def function_name(...):)
        Pattern pattern = Pattern.compile(
            "def\\s+(\\w+)\\s*\\([^)]*\\)\\s*:",
            Pattern.MULTILINE
        );
        
        Matcher matcher = pattern.matcher(code);
        
        while (matcher.find()) {
            String functionName = matcher.group(1);
            int start = matcher.start();
            
            // Находим конец функции (следующая функция или конец файла)
            int end = findPythonFunctionEnd(code, matcher.end());
            
            String functionBody = code.substring(start, end);
            functions.add(new CodeParser.FunctionInfo(functionName, functionBody, start, end));
            log.debug("Found Python function: {} at position {}-{}", functionName, start, end);
        }
        
        if (functions.isEmpty()) {
            log.warn("No Python functions found in file: {}. Returning full content as single function.", codeSource.getFileName());
            functions.add(new CodeParser.FunctionInfo("main", code, 0, code.length()));
        }
        
        log.info("Parsed {} Python functions from file: {}", functions.size(), codeSource.getFileName());
        return functions;
    }
    
    @Override
    public String getSupportedLanguage() {
        return "python";
    }
    
    private int findPythonFunctionEnd(String code, int startPos) {
        int pos = startPos;
        int baseIndent = -1;
        
        // Находим базовый отступ функции
        while (pos < code.length()) {
            char c = code.charAt(pos);
            if (c == '\n') {
                pos++;
                int indent = 0;
                while (pos < code.length() && code.charAt(pos) == ' ') {
                    indent++;
                    pos++;
                }
                if (baseIndent == -1 && pos < code.length() && code.charAt(pos) != '\n') {
                    baseIndent = indent;
                }
                if (baseIndent != -1 && indent <= baseIndent && pos < code.length() && code.charAt(pos) != '\n' && code.charAt(pos) != '#') {
                    // Нашли следующую функцию или конец
                    return pos - indent - 1;
                }
            }
            pos++;
        }
        
        return code.length();
    }
}
