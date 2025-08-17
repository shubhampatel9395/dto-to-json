package com.project.controller;

import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.project.exception.BaseException;
import com.project.exception.BaseRuntimeException;
import com.project.locale.MessageByLocaleService;
import com.project.response.handler.GenericResponseHandlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice(basePackages = "com.project")
@AllArgsConstructor
@Slf4j
public class ErrorHandlingController {

	private final MessageByLocaleService messageByLocaleService;

	/**
	 * Central exception handler and generate common custom response
	 *
	 * @param request
	 * @param exception
	 * @return
	 */
	@ExceptionHandler(Throwable.class)
	@ResponseBody
	ResponseEntity<Object> handleControllerException(final HttpServletRequest request, final Throwable exception,
			final Locale locale) {
		HttpStatus status = null;
		String message;
		StringBuffer requestedURL = request.getRequestURL();
		if (exception instanceof BaseException baseException) {
			status = baseException.getStatus();
			message = baseException.getMessage();
		} else if (exception instanceof BaseRuntimeException baseRuntimeException) {
			status = baseRuntimeException.getStatus();
			message = baseRuntimeException.getMessage();
		} else if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
			message = methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
					.map(FieldError::getDefaultMessage).collect(Collectors.joining(","));
			status = HttpStatus.BAD_REQUEST;
		} else if (exception instanceof AccessDeniedException || exception instanceof AuthenticationException) {
			status = HttpStatus.UNAUTHORIZED;
			message = exception.getMessage();
		} else if (exception instanceof MissingServletRequestParameterException
				|| exception instanceof MissingRequestHeaderException
				|| exception instanceof HttpMessageNotReadableException) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			message = exception.getMessage();
			log.info("Requested URL:{}", requestedURL);
			log.error("exception : {}", exception);
		} else if (exception instanceof HttpClientErrorException || exception instanceof HttpServerErrorException) {
			if (exception.getMessage().contains("401")) {
				status = HttpStatus.UNAUTHORIZED;
				message = messageByLocaleService.getMessage("invalid.username.password", null);
			} else {
				status = HttpStatus.INTERNAL_SERVER_ERROR;
				message = exception.getMessage();
				log.info("Requested URL:{}", requestedURL);
				log.error("exception : {}", exception);
			}
		} else if (exception instanceof MethodArgumentTypeMismatchException) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			message = "Argument mis matched";
			log.info("Requested URL:{}", requestedURL);
			log.error("exception : {}", exception);
		} else if (exception instanceof ObjectOptimisticLockingFailureException) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			message = "Something went wrong. Please try again";
			log.info("Requested URL:{}", requestedURL);
			log.error("exception : {}", exception);
		} else {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			message = messageByLocaleService.getMessage("common.error", null);
			log.info("Requested URL:{}", requestedURL);
			log.error("exception : {}", exception);
		}
		return new GenericResponseHandlers.Builder().setStatus(status).setMessage(message).create();
	}

}
