package com.project.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Author: Kody Technolab Ltd. <br/>
 * Date : 09-May-2024
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**",
								"/swagger-ui.html", "/api-docs.html", "/api-docs/**")
						.permitAll().anyRequest().permitAll())
				.httpBasic(withDefaults()).build();
	}
}