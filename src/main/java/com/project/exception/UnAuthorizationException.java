package com.project.exception;

import org.springframework.http.HttpStatus;

/**
 * 
 *  Author: Kody Technolab Ltd. <br/>
 *  Date : 13-May-2024
 */
public class UnAuthorizationException extends BaseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5393966066293603049L;
	private static final HttpStatus status = HttpStatus.UNAUTHORIZED;

	/**
	 *
	 */
	public UnAuthorizationException() {
	}

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public UnAuthorizationException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public UnAuthorizationException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public UnAuthorizationException(final Throwable cause) {
		super(status, cause);
	}

}
