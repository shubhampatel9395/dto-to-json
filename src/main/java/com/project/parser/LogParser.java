package com.project.parser;

public interface LogParser {
    boolean supports(String input);
    Object parseToJson(String input) throws Exception;
}
