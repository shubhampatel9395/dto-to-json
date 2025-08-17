package com.project.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidatorImpl implements ConstraintValidator<EnumValidator, String> {
	private Class<? extends Enum<?>> enumClass;

	@Override
	public void initialize(EnumValidator annotation) {
		this.enumClass = annotation.enumClass();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true; // Use @NotNull for null checks
		}

		for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
			if (enumConstant.name().equals(value)) {
				return true;
			}
		}

		return false;
	}

}