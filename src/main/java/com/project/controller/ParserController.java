package com.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dto.RequestDTO;
import com.project.locale.MessageByLocaleService;
import com.project.response.handler.GenericResponseHandlers;
import com.project.service.ParserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/parser")
@Slf4j
@RequiredArgsConstructor
public class ParserController {

	/**
	 * Locale message service - to display response messages from Property file
	 */
	private final MessageByLocaleService messageByLocaleService;
	
	private final ParserService parserService;

	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getJson(@Valid @RequestBody final RequestDTO requestDTO) throws Exception {
		Object response = parserService.parseService(requestDTO);
		return new GenericResponseHandlers.Builder().setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("detail.message", null)).setData(response).create();
	}

}
