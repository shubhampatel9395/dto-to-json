package com.project.exception;

import org.springframework.http.HttpStatus;

/**
 * 
 *  Author: Kody Technolab Ltd. <br/>
 *  Date : 13-May-2024
 */
public class AuthorizationException extends BaseException {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -522064384771001561L;
	private static final HttpStatus status = HttpStatus.UNAUTHORIZED;

	/**
	 *
	 */
	public AuthorizationException() {
	}

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public AuthorizationException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public AuthorizationException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public AuthorizationException(final Throwable cause) {
		super(status, cause);
	}

}
