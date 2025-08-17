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

@RequiredArgsConstructor
@Service(value = Constant.CSHARP)
public class CSharpDTOParser implements LogParser {
    
    // Enhanced regex patterns for C# type detection
    private static final Pattern CLASS_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\{.*\\}$");
    private static final Pattern ANONYMOUS_PATTERN = Pattern.compile("^\\{.*\\}$");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\".*\"$");
    private static final Pattern CHAR_PATTERN = Pattern.compile("^'.'$");
    private static final Pattern VERBATIM_STRING_PATTERN = Pattern.compile("^@\".*\"$");
    private static final Pattern INTERPOLATED_STRING_PATTERN = Pattern.compile("^\\$\".*\"$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+[fFdDmM]?$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+[lLuUfF]?$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false|True|False)$");
    private static final Pattern NULL_PATTERN = Pattern.compile("^(null|NULL)$");
    private static final Pattern GUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern HEX_PATTERN = Pattern.compile("^0[xX][0-9a-fA-F]+$");
    private static final Pattern ENUM_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9_]*\\.[A-Z_][A-Z0-9_]*$");
    private static final Pattern DATETIME_PATTERN = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}");

    @Override
    public boolean supports(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = input.trim();
        // Enhanced pattern matching for C# objects
        return CLASS_PATTERN.matcher(trimmed).matches() ||
               ANONYMOUS_PATTERN.matcher(trimmed).matches() ||
               trimmed.startsWith("new ") ||
               trimmed.startsWith("new[") ||
               trimmed.startsWith("List<") ||
               trimmed.startsWith("Dictionary<") ||
               trimmed.startsWith("Array[") ||
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
            throw new Exception("Failed to parse C# DTO: " + e.getMessage(), e);
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
            return Boolean.parseBoolean(value.toLowerCase());
        }
        
        // Handle verbatim strings (@"...")
        if (VERBATIM_STRING_PATTERN.matcher(value).matches()) {
            return value.substring(2, value.length() - 1).replace("\"\"", "\"");
        }
        
        // Handle interpolated strings ($"...")
        if (INTERPOLATED_STRING_PATTERN.matcher(value).matches()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__type", "interpolated_string");
            result.put("value", value.substring(2, value.length() - 1));
            return result;
        }
        
        // Handle regular strings (quoted)
        if (STRING_PATTERN.matcher(value).matches()) {
            return unescapeCSharpString(value.substring(1, value.length() - 1));
        }
        
        // Handle character
        if (CHAR_PATTERN.matcher(value).matches()) {
            return value.charAt(1); // Extract character between quotes
        }
        
        // Handle GUID
        if (GUID_PATTERN.matcher(value).matches()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__type", "Guid");
            result.put("value", value);
            return result;
        }
        
        // Handle DateTime
        if (DATETIME_PATTERN.matcher(value).matches()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__type", "DateTime");
            result.put("value", value);
            return result;
        }
        
        // Handle hexadecimal numbers
        if (HEX_PATTERN.matcher(value).matches()) {
            return Long.parseLong(value.substring(2), 16);
        }
        
        // Handle decimal numbers
        if (DECIMAL_PATTERN.matcher(value).matches()) {
            return parseCSharpDecimalNumber(value);
        }
        
        // Handle integer numbers
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return parseCSharpIntegerNumber(value);
        }
        
        // Handle C# object instantiation (new ClassName { ... })
        if (value.startsWith("new ") && value.contains("{") && value.endsWith("}")) {
            return parseCSharpObjectInitializer(value);
        }
        
        // Handle C# List initialization (new List<T> { ... })
        if (value.startsWith("new List<") && value.endsWith("}")) {
            return parseCSharpListInitializer(value);
        }
        
        // Handle C# Dictionary initialization
        if (value.startsWith("new Dictionary<") && value.endsWith("}")) {
            return parseCSharpDictionaryInitializer(value);
        }
        
        // Handle C# Array initialization
        if (value.startsWith("new ") && value.contains("[") && value.endsWith("]")) {
            return parseCSharpArrayInitializer(value);
        }
        
        // Handle anonymous objects { prop = value }
        if (ANONYMOUS_PATTERN.matcher(value).matches()) {
            return parseCSharpAnonymousObject(value);
        }
        
        // Handle regular arrays/lists
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseCSharpArray(value.substring(1, value.length() - 1));
        }
        
        // Handle C# class objects (ClassName { ... })
        if (CLASS_PATTERN.matcher(value).matches()) {
            return parseCSharpClassObject(value);
        }
        
        // Handle enums (Enum.Value)
        if (ENUM_PATTERN.matcher(value).matches()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__type", "enum");
            String[] parts = value.split("\\.", 2);
            result.put("enum_type", parts[0]);
            result.put("value", parts[1]);
            return result;
        }
        
        // Handle C# nullable types
        if (value.equals("null")) {
            return null;
        }
        
        // Default: treat as unquoted string
        return value;
    }
    
    private Map<String, Object> parseCSharpObjectInitializer(String input) throws Exception {
        // Parse "new ClassName { prop1 = value1, prop2 = value2 }"
        int newIndex = input.indexOf("new ");
        int braceIndex = input.indexOf("{");
        
        if (newIndex == -1 || braceIndex == -1) {
            throw new Exception("Invalid C# object initializer format: " + input);
        }
        
        String className = input.substring(4, braceIndex).trim();
        String content = input.substring(braceIndex + 1, input.lastIndexOf("}"));
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("__class", className);
        result.put("__type", "object_initializer");
        
        if (!content.trim().isEmpty()) {
            parseCSharpProperties(content, result);
        }
        
        return result;
    }
    
    private Map<String, Object> parseCSharpListInitializer(String input) throws Exception {
        // Parse "new List<T> { item1, item2, item3 }"
        int braceIndex = input.indexOf("{");
        String typeInfo = input.substring(4, braceIndex).trim(); // Remove "new "
        String content = input.substring(braceIndex + 1, input.lastIndexOf("}"));
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("__type", "List");
        result.put("generic_type", typeInfo);
        result.put("items", parseCSharpArray(content));
        
        return result;
    }
    
    private Map<String, Object> parseCSharpDictionaryInitializer(String input) throws Exception {
        // Parse "new Dictionary<K,V> { {key1, value1}, {key2, value2} }"
        int braceIndex = input.indexOf("{");
        String typeInfo = input.substring(4, braceIndex).trim(); // Remove "new "
        String content = input.substring(braceIndex + 1, input.lastIndexOf("}"));
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("__type", "Dictionary");
        result.put("generic_type", typeInfo);
        result.put("entries", parseCSharpDictionary(content));
        
        return result;
    }
    
    private Map<String, Object> parseCSharpArrayInitializer(String input) throws Exception {
        // Parse "new int[] { 1, 2, 3 }" or "new[] { 1, 2, 3 }"
        int braceStart = input.indexOf("{");
        int braceEnd = input.lastIndexOf("}");
        
        if (braceStart == -1 || braceEnd == -1) {
            // Handle "new int[3]" format
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__type", "Array");
            result.put("declaration", input);
            return result;
        }
        
        String typeInfo = input.substring(4, braceStart).trim(); // Remove "new "
        String content = input.substring(braceStart + 1, braceEnd);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("__type", "Array");
        result.put("array_type", typeInfo);
        result.put("items", parseCSharpArray(content));
        
        return result;
    }
    
    private Map<String, Object> parseCSharpAnonymousObject(String input) throws Exception {
        // Parse "{ prop1 = value1, prop2 = value2 }"
        String content = input.substring(1, input.length() - 1);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("__type", "anonymous");
        
        if (!content.trim().isEmpty()) {
            parseCSharpProperties(content, result);
        }
        
        return result;
    }
    
    private Map<String, Object> parseCSharpClassObject(String input) throws Exception {
        // Parse "ClassName { prop1 = value1, prop2 = value2 }"
        int braceIndex = input.indexOf("{");
        String className = input.substring(0, braceIndex).trim();
        String content = input.substring(braceIndex + 1, input.lastIndexOf("}"));
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("__class", className);
        result.put("__type", "class");
        
        if (!content.trim().isEmpty()) {
            parseCSharpProperties(content, result);
        }
        
        return result;
    }
    
    private void parseCSharpProperties(String content, Map<String, Object> result) throws Exception {
        List<String> properties = splitTopLevelCommaSeparated(content);
        
        for (String property : properties) {
            String[] kv = property.split("=", 2);
            if (kv.length != 2) {
                throw new Exception("Invalid C# property format: " + property);
            }
            
            String key = kv[0].trim();
            String value = kv[1].trim();
            
            if (key.isEmpty()) {
                throw new Exception("Empty property name in: " + property);
            }
            
            result.put(key, parseValue(value));
        }
    }

    private List<String> splitTopLevelCommaSeparated(String input) {
        List<String> result = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return result;
        }
        
        int level = 0;
        int start = 0;
        boolean inQuotes = false;
        boolean inVerbatimString = false;
        char quoteChar = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            
            // Handle verbatim strings (@"...")
            if (ch == '@' && i < input.length() - 1 && input.charAt(i + 1) == '"' && !inQuotes) {
                inVerbatimString = true;
                i++; // Skip the quote
                continue;
            }
            
            // Handle end of verbatim string
            if (inVerbatimString && ch == '"') {
                // Check if it's escaped (double quote)
                if (i < input.length() - 1 && input.charAt(i + 1) == '"') {
                    i++; // Skip escaped quote
                    continue;
                } else {
                    inVerbatimString = false;
                    continue;
                }
            }
            
            if (inVerbatimString) {
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

    private List<Object> parseCSharpArray(String value) throws Exception {
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

    private Map<String, Object> parseCSharpDictionary(String value) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (value.trim().isEmpty()) {
            return result;
        }
        
        // Handle different dictionary initialization formats
        List<String> entries = splitTopLevelCommaSeparated(value);
        for (String entry : entries) {
            // Format: {key, value}
            if (entry.trim().startsWith("{") && entry.trim().endsWith("}")) {
                String content = entry.trim().substring(1, entry.trim().length() - 1);
                String[] kv = content.split(",", 2);
                if (kv.length == 2) {
                    String key = parseMapKey(kv[0].trim());
                    Object val = parseValue(kv[1].trim());
                    result.put(key, val);
                }
            }
            // Format: [key] = value
            else if (entry.contains("] =")) {
                int equalIndex = entry.indexOf("] =");
                String keyPart = entry.substring(0, equalIndex + 1).trim();
                String valuePart = entry.substring(equalIndex + 2).trim();
                
                // Extract key from [key] format
                if (keyPart.startsWith("[") && keyPart.endsWith("]")) {
                    String key = parseMapKey(keyPart.substring(1, keyPart.length() - 1));
                    Object val = parseValue(valuePart);
                    result.put(key, val);
                }
            }
            // Format: key = value
            else if (entry.contains("=")) {
                String[] kv = entry.split("=", 2);
                if (kv.length == 2) {
                    String key = parseMapKey(kv[0].trim());
                    Object val = parseValue(kv[1].trim());
                    result.put(key, val);
                }
            }
        }
        
        return result;
    }
    
    private String parseMapKey(String key) {
        // Remove quotes if present
        if (STRING_PATTERN.matcher(key).matches()) {
            return key.substring(1, key.length() - 1);
        }
        if (VERBATIM_STRING_PATTERN.matcher(key).matches()) {
            return key.substring(2, key.length() - 1).replace("\"\"", "\"");
        }
        return key;
    }
    
    private Object parseCSharpDecimalNumber(String value) {
        // Remove suffix if present
        String cleanValue = value.replaceAll("[fFdDmM]$", "");
        
        if (value.endsWith("f") || value.endsWith("F")) {
            return Float.parseFloat(cleanValue);
        } else if (value.endsWith("d") || value.endsWith("D")) {
            return Double.parseDouble(cleanValue);
        } else if (value.endsWith("m") || value.endsWith("M")) {
            // C# decimal type
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__type", "decimal");
            result.put("value", new BigDecimal(cleanValue));
            return result;
        } else {
            // Default to Double for decimal numbers
            try {
                return Double.parseDouble(cleanValue);
            } catch (NumberFormatException e) {
                return new BigDecimal(cleanValue);
            }
        }
    }
    
    private Object parseCSharpIntegerNumber(String value) {
        // Handle unsigned suffix
        if (value.endsWith("u") || value.endsWith("U")) {
            String cleanValue = value.substring(0, value.length() - 1);
            return Long.parseUnsignedLong(cleanValue);
        }
        
        // Handle long suffix
        if (value.endsWith("l") || value.endsWith("L")) {
            String cleanValue = value.substring(0, value.length() - 1);
            return Long.parseLong(cleanValue);
        }
        
        // Handle float suffix (for whole numbers)
        if (value.endsWith("f") || value.endsWith("F")) {
            String cleanValue = value.substring(0, value.length() - 1);
            return Float.parseFloat(cleanValue);
        }
        
        try {
            // Try int first, then long
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e2) {
                return new BigInteger(value);
            }
        }
    }
    
    private String unescapeCSharpString(String str) {
        return str.replace("\\\"", "\"")
                  .replace("\\'", "'")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\0", "\0")
                  .replace("\\a", "\u0007")  // Alert (bell)
                  .replace("\\b", "\b")      // Backspace
                  .replace("\\f", "\f")      // Form feed
                  .replace("\\v", "\u000B"); // Vertical tab
    }
}