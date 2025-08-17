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

@Service(value = Constant.KOTLIN)
@RequiredArgsConstructor
public class KotlinDTOParser implements LogParser {
    
    // Enhanced regex patterns for Kotlin type detection
    private static final Pattern DATA_CLASS_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*\\(.*\\)$");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\".*\"$");
    private static final Pattern CHAR_PATTERN = Pattern.compile("^'.'$");
    private static final Pattern RAW_STRING_PATTERN = Pattern.compile("^\"\"\"[\\s\\S]*\"\"\"$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+[fFdD]?$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+[lLuU]?$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$");
    private static final Pattern NULL_PATTERN = Pattern.compile("^null$");
    private static final Pattern ENUM_PATTERN = Pattern.compile("^[A-Z_][A-Z0-9_]*$");
    private static final Pattern UNSIGNED_PATTERN = Pattern.compile("^\\d+[uU]$");
    private static final Pattern HEX_PATTERN = Pattern.compile("^0[xX][0-9a-fA-F]+$");
    private static final Pattern BINARY_PATTERN = Pattern.compile("^0[bB][01]+$");

    @Override
    public boolean supports(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = input.trim();
        // Enhanced pattern matching for Kotlin data classes
        return DATA_CLASS_PATTERN.matcher(trimmed).matches() ||
               trimmed.startsWith("listOf(") ||
               trimmed.startsWith("mapOf(") ||
               trimmed.startsWith("setOf(") ||
               trimmed.startsWith("arrayOf(") ||
               trimmed.startsWith("mutableListOf(") ||
               trimmed.startsWith("mutableMapOf(") ||
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
            throw new Exception("Failed to parse Kotlin DTO: " + e.getMessage(), e);
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
        
        // Handle Kotlin raw strings (triple quotes)
        if (RAW_STRING_PATTERN.matcher(value).matches()) {
            return value.substring(3, value.length() - 3);
        }
        
        // Handle regular strings (quoted)
        if (STRING_PATTERN.matcher(value).matches()) {
            return unescapeKotlinString(value.substring(1, value.length() - 1));
        }
        
        // Handle character
        if (CHAR_PATTERN.matcher(value).matches()) {
            return value.charAt(1); // Extract character between quotes
        }
        
        // Handle hexadecimal numbers
        if (HEX_PATTERN.matcher(value).matches()) {
            return Long.parseLong(value.substring(2), 16);
        }
        
        // Handle binary numbers
        if (BINARY_PATTERN.matcher(value).matches()) {
            return Long.parseLong(value.substring(2), 2);
        }
        
        // Handle unsigned numbers
        if (UNSIGNED_PATTERN.matcher(value).matches()) {
            String numPart = value.substring(0, value.length() - 1);
            return Long.parseUnsignedLong(numPart);
        }
        
        // Handle decimal numbers
        if (DECIMAL_PATTERN.matcher(value).matches()) {
            return parseKotlinDecimalNumber(value);
        }
        
        // Handle integer numbers
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return parseKotlinIntegerNumber(value);
        }
        
        // Handle Kotlin collection functions
        if (value.startsWith("listOf(") && value.endsWith(")")) {
            String content = value.substring(7, value.length() - 1);
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "List");
//            result.put("items", parseKotlinList(content));
            return parseKotlinList(content);
        }
        
        if (value.startsWith("mutableListOf(") && value.endsWith(")")) {
            String content = value.substring(14, value.length() - 1);
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "MutableList");
//            result.put("items", parseKotlinList(content));
            return parseKotlinList(content);
        }
        
        if (value.startsWith("setOf(") && value.endsWith(")")) {
            String content = value.substring(6, value.length() - 1);
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "Set");
//            result.put("items", parseKotlinList(content));
            return parseKotlinList(content);
        }
        
        if (value.startsWith("mapOf(") && value.endsWith(")")) {
            String content = value.substring(6, value.length() - 1);
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "Map");
//            result.put("entries", parseKotlinMap(content));
            return parseKotlinMap(content);
        }
        
        if (value.startsWith("mutableMapOf(") && value.endsWith(")")) {
            String content = value.substring(13, value.length() - 1);
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "MutableMap");
//            result.put("entries", parseKotlinMap(content));
            return parseKotlinMap(content);
        }
        
        if (value.startsWith("arrayOf(") && value.endsWith(")")) {
            String content = value.substring(8, value.length() - 1);
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "Array");
//            result.put("items", parseKotlinList(content));
            return parseKotlinList(content);
        }
        
        // Handle Kotlin data class objects
        if (DATA_CLASS_PATTERN.matcher(value).matches()) {
            return parseKotlinDataClass(value);
        }
        
        // Handle regular arrays/lists
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseKotlinList(value.substring(1, value.length() - 1));
        }
        
        // Handle maps
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseKotlinMap(value.substring(1, value.length() - 1));
        }
        
        // Handle Kotlin ranges
        if (value.contains("..")) {
            return parseKotlinRange(value);
        }
        
        // Handle enums (assume uppercase constants are enums)
        if (ENUM_PATTERN.matcher(value).matches()) {
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "enum");
//            result.put("value", value);
            return value;
        }
        
        // Handle Kotlin lambda expressions (basic detection)
        if (value.startsWith("{") && value.contains("->")) {
//            Map<String, Object> result = new LinkedHashMap<>();
//            result.put("__type", "lambda");
//            result.put("expression", value);
            return value;
        }
        
        // Default: treat as unquoted string
        return value;
    }
    
    private Map<String, Object> parseKotlinDataClass(String input) throws Exception {
        int openIdx = input.indexOf('(');
        int closeIdx = input.lastIndexOf(')');
        
        if (openIdx == -1 || closeIdx == -1) {
            throw new Exception("Invalid Kotlin data class format: " + input);
        }
        
//        String className = input.substring(0, openIdx);
        String inside = input.substring(openIdx + 1, closeIdx);

        Map<String, Object> result = new LinkedHashMap<>();
//        result.put("__class", className);
//        result.put("__type", "data_class");

        if (inside.trim().isEmpty()) {
            return result;
        }

        List<String> fields = splitTopLevelCommaSeparated(inside);
        for (String field : fields) {
            parseKotlinField(field, result);
        }
        
        return result;
    }
    
    private void parseKotlinField(String field, Map<String, Object> result) throws Exception {
        String[] kv = field.split("=", 2);
        if (kv.length != 2) {
            throw new Exception("Invalid Kotlin field format: " + field);
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
        boolean inRawString = false;
        char quoteChar = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            
            // Handle raw strings (triple quotes)
            if (i <= input.length() - 3 && input.substring(i, i + 3).equals("\"\"\"")) {
                if (!inRawString) {
                    inRawString = true;
                    i += 2; // Skip next 2 characters
                    continue;
                } else {
                    inRawString = false;
                    i += 2; // Skip next 2 characters
                    continue;
                }
            }
            
            if (inRawString) {
                continue;
            }
            
            // Handle regular quotes
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

    private List<Object> parseKotlinList(String value) throws Exception {
        List<Object> result = new ArrayList<>();
        
        if (value.trim().isEmpty()) {
            return result;
        }
        
        List<String> elements = splitTopLevelCommaSeparated(value);
        for (String element : elements) {
            result.add(parseValue(element.trim()));
        }
        
        return result;
    }

    private Map<String, Object> parseKotlinMap(String value) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (value.trim().isEmpty()) {
            return result;
        }
        
        List<String> entries = splitTopLevelCommaSeparated(value);
        for (String entry : entries) {
            // Handle Kotlin's "to" syntax: key to value
            if (entry.contains(" to ")) {
                String[] kv = entry.split(" to ", 2);
                if (kv.length == 2) {
                    String key = parseMapKey(kv[0].trim());
                    Object val = parseValue(kv[1].trim());
                    result.put(key, val);
                }
            } 
            // Handle regular key=value syntax
            else if (entry.contains("=")) {
                String[] kv = entry.split("=", 2);
                if (kv.length == 2) {
                    String key = parseMapKey(kv[0].trim());
                    Object val = parseValue(kv[1].trim());
                    result.put(key, val);
                }
            }
            else {
                throw new Exception("Invalid Kotlin map entry format: " + entry);
            }
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
    
    private Object parseKotlinRange(String value) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
//        result.put("__type", "range");
        
        if (value.contains("..")) {
            String[] parts = value.split("\\.\\.", 2);
            if (parts.length == 2) {
                result.put("start", parseValue(parts[0].trim()));
                result.put("end", parseValue(parts[1].trim()));
            }
        }
        
        return result;
    }
    
    private Object parseKotlinDecimalNumber(String value) {
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
    
    private Object parseKotlinIntegerNumber(String value) {
        // Handle unsigned suffix
        if (value.endsWith("u") || value.endsWith("U")) {
            String cleanValue = value.substring(0, value.length() - 1);
            return Long.parseUnsignedLong(cleanValue);
        }
        
        // Remove long suffix if present
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
    
    private String unescapeKotlinString(String str) {
        return str.replace("\\\"", "\"")
                  .replace("\\'", "'")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\$", "$")
                  .replace("\\{", "{")
                  .replace("\\}", "}");
    }
}