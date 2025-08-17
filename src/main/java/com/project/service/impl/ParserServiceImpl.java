package com.project.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.constant.InputLanguage;
import com.project.dto.RequestDTO;
import com.project.exception.ValidationException;
import com.project.locale.MessageByLocaleService;
import com.project.parser.LogParser;
import com.project.service.ParserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional(rollbackFor = Throwable.class)
@RequiredArgsConstructor
@Slf4j
@Service
public class ParserServiceImpl implements ParserService {

	private final MessageByLocaleService messageByLocaleService;
	private final Map<String, LogParser> logParsers;

	@Override
	public Object parseService(final RequestDTO requestDTO) throws Exception {
		log.info("Inside ParserServiceImpl::parseService, {}", requestDTO);

		InputLanguage language = InputLanguage.getByValue(requestDTO.getLanguage());

		LogParser parser = logParsers.get(language.getValue().toUpperCase());
		if (parser == null) {
			throw new ValidationException(messageByLocaleService.getMessage("invalid.language", null));
		}
		
		String input = requestDTO.getInputTxt().trim();
		if (!parser.supports(input)) {
			throw new ValidationException(messageByLocaleService.getMessage("can.not.parse.selected.language", null));
        }

		return parser.parseToJson(input);
	}

}
