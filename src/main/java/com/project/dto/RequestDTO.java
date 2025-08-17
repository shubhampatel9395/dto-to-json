package com.project.dto;

import java.io.Serializable;

import com.project.annotation.EnumValidator;
import com.project.constant.InputLanguage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestDTO implements Serializable {

	private static final long serialVersionUID = 7415991367149254140L;

	@NotNull(message = "{language.not.null}")
	@EnumValidator(enumClass = InputLanguage.class, message = "{language.invalid}")
	private String language;
	
	@NotBlank
	private String inputTxt;
}
