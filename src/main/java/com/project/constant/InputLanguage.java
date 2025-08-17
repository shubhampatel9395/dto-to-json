package com.project.constant;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 
 *  Author: Kody Technolab Ltd. <br/>
 *  Date : 13-May-2024
 */
@Getter
@AllArgsConstructor
public enum InputLanguage {

	JAVA("JAVA"), KOTLIN("KOTLIN");

	String value;

	private static final Map<String, InputLanguage> LANGUAGE_MAP = new HashMap<>();
	static {
		for (final InputLanguage value : values()) {
			LANGUAGE_MAP.put(value.getValue(), value);
		}
	}

	public static InputLanguage getByValue(final String value) {
		return LANGUAGE_MAP.get(value);
	}

}
