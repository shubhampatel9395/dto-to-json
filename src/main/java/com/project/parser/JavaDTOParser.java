package com.project.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.project.util.Constant;

import lombok.RequiredArgsConstructor;

@Service(value = Constant.JAVA)
@RequiredArgsConstructor
public class JavaDTOParser implements LogParser {
    
    // Enhanced regex patterns for better Java type detection
    private static final Pattern DTO_PATTERN = Pattern.compile("^\\w+\\(.*\\)$");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\".*\"$|^'.*'$");
    private static final Pattern CHAR_PATTERN = Pattern.compile("^'.'$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+[fFdD]?$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+[lL]?$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$");
    private static final Pattern NULL_PATTERN = Pattern.compile("^null$");
    private static final Pattern ENUM_PATTERN = Pattern.compile("^[A-Z_][A-Z0-9_]*$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATETIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    
    @Override
    public boolean supports(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = input.trim();
        // Enhanced pattern matching for Java DTOs
        return DTO_PATTERN.matcher(trimmed).matches() ||
               trimmed.startsWith("{") && trimmed.endsWith("}") ||
               trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    @Override
    public Object parseToJson(String input) throws Exception {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        
        try {
            return parseValue(input.trim());
        } catch (Exception e) {
            throw new Exception("Failed to parse Java DTO: " + e.getMessage(), e);
        }
    }

    private Object parseValue(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        // Handle null
        if (NULL_PATTERN.matcher(value).matches()) {
            return null;
        }
        
        // Handle boolean
        if (BOOLEAN_PATTERN.matcher(value).matches()) {
            return Boolean.parseBoolean(value);
        }
        
        // Handle strings (quoted)
        if (STRING_PATTERN.matcher(value).matches()) {
            return unescapeString(value.substring(1, value.length() - 1));
        }
        
        // Handle character
        if (CHAR_PATTERN.matcher(value).matches()) {
            return value.charAt(1); // Extract character between quotes
        }
        
        // Handle numbers
        if (DECIMAL_PATTERN.matcher(value).matches()) {
            return parseDecimalNumber(value);
        }
        
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return parseIntegerNumber(value);
        }
        
        // Handle UUID
        if (UUID_PATTERN.matcher(value).matches()) {
            return value; // Keep as string or convert to UUID object
        }
        
        // Handle Date/DateTime
        if (DATE_PATTERN.matcher(value).matches()) {
            return value; // Keep as string or parse to LocalDate
        }
        
        if (DATETIME_PATTERN.matcher(value).matches()) {
            return value; // Keep as string or parse to LocalDateTime
        }
        
        // Handle Java DTO objects
        if (DTO_PATTERN.matcher(value).matches()) {
            return parseObject(value);
        }
        
        // Handle arrays/lists
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseArray(value);
        }
        
        // Handle maps
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseMap(value);
        }
        
        // Handle enums (assume uppercase constants are enums)
        if (ENUM_PATTERN.matcher(value).matches()) {
           //  return Map.of("__type", "enum", "value", value);
        	return value;
        }
        
        // Default: treat as unquoted string
        return value;
    }
    
    private Map<String, Object> parseObject(String input) throws Exception {
        int openIdx = input.indexOf('(');
        int closeIdx = input.lastIndexOf(')');
        
        if (openIdx == -1 || closeIdx == -1) {
            throw new Exception("Invalid object format: " + input);
        }
        
        // String className = input.substring(0, openIdx);
        String inside = input.substring(openIdx + 1, closeIdx);

        Map<String, Object> result = new LinkedHashMap<>();
        // result.put("__class", className);

        if (inside.trim().isEmpty()) {
            return result;
        }

        List<String> fields = splitTopLevelCommaSeparated(inside);
        for (String field : fields) {
            parseField(field, result);
        }
        
        return result;
    }
    
    private void parseField(String field, Map<String, Object> result) throws Exception {
        String[] kv = field.split("=", 2);
        if (kv.length != 2) {
            throw new Exception("Invalid field format: " + field);
        }
        
        String key = kv[0].trim();
        String value = kv[1].trim();
        
        // Validate key
        if (key.isEmpty()) {
            throw new Exception("Empty field name in: " + field);
        }
        
        result.put(key, parseValue(value));
    }

    private List<String> splitTopLevelCommaSeparated(String input) {
        List<String> result = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return result;
        }
        
        int level = 0;
        int start = 0;
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            
            // Handle quotes
            if ((ch == '"' || ch == '\'') && (i == 0 || input.charAt(i-1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = ch;
                } else if (ch == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }
            
            if (inQuotes) {
                continue;
            }
            
            // Handle brackets and parentheses
            if (ch == '(' || ch == '[' || ch == '{') {
                level++;
            } else if (ch == ')' || ch == ']' || ch == '}') {
                level--;
            } else if (ch == ',' && level == 0) {
                String segment = input.substring(start, i).trim();
                if (!segment.isEmpty()) {
                    result.add(segment);
                }
                start = i + 1;
            }
        }
        
        // Add the last segment
        String lastSegment = input.substring(start).trim();
        if (!lastSegment.isEmpty()) {
            result.add(lastSegment);
        }
        
        return result;
    }

    private List<Object> parseArray(String value) throws Exception {
        String content = value.substring(1, value.length() - 1).trim();
        List<Object> result = new ArrayList<>();
        
        if (content.isEmpty()) {
            return result;
        }
        
        List<String> elements = splitTopLevelCommaSeparated(content);
        for (String element : elements) {
            result.add(parseValue(element.trim()));
        }
        
        return result;
    }

    private Map<String, Object> parseMap(String value) throws Exception {
        String content = value.substring(1, value.length() - 1).trim();
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (content.isEmpty()) {
            return result;
        }
        
        List<String> entries = splitTopLevelCommaSeparated(content);
        for (String entry : entries) {
            String[] kv = entry.split("=", 2);
            if (kv.length != 2) {
                throw new Exception("Invalid map entry format: " + entry);
            }
            
            String key = parseMapKey(kv[0].trim());
            Object val = parseValue(kv[1].trim());
            result.put(key, val);
        }
        
        return result;
    }
    
    private String parseMapKey(String key) {
        // Remove quotes if present
        if (STRING_PATTERN.matcher(key).matches()) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }
    
    private Object parseDecimalNumber(String value) {
        // Remove suffix if present
        String cleanValue = value.replaceAll("[fFdD]$", "");
        
        if (value.endsWith("f") || value.endsWith("F")) {
            return Float.parseFloat(cleanValue);
        } else if (value.endsWith("d") || value.endsWith("D")) {
            return Double.parseDouble(cleanValue);
        } else {
            // Default to Double for decimal numbers
            try {
                return Double.parseDouble(cleanValue);
            } catch (NumberFormatException e) {
                return new BigDecimal(cleanValue);
            }
        }
    }
    
    private Object parseIntegerNumber(String value) {
        // Remove suffix if present
        String cleanValue = value.replaceAll("[lL]$", "");
        
        try {
            if (value.endsWith("l") || value.endsWith("L")) {
                return Long.parseLong(cleanValue);
            } else {
                // Try int first, then long
                try {
                    return Integer.parseInt(cleanValue);
                } catch (NumberFormatException e) {
                    return Long.parseLong(cleanValue);
                }
            }
        } catch (NumberFormatException e) {
            return new BigInteger(cleanValue);
        }
    }
    
    private String unescapeString(String str) {
        return str.replace("\\\"", "\"")
                  .replace("\\'", "'")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
}
