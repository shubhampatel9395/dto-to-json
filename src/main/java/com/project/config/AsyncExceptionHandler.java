package com.project.config;

import java.lang.reflect.Method;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

	@Override
	public void handleUncaughtException(final Throwable ex, final Method method, final Object... params) {
		log.error(" Async exception ", ex);
		log.error("Async exception source method {}", method.getName());
		for (Object param : params) {
			log.error("Async exception method param {}", param);
		}
	}
}