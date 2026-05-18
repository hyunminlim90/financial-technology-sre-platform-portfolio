package com.fintech.sre.agent.governance.timeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DefaultGovernanceTimelineCursorCodec
		implements GovernanceTimelineCursorCodec {

	private final ObjectMapper objectMapper;

	public DefaultGovernanceTimelineCursorCodec(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String encode(GovernanceTimelineCursor cursor) {
		if (cursor == null) {
			throw new GovernanceTimelineCursorDecodeException();
		}

		try {
			byte[] json = objectMapper.writeValueAsBytes(cursor);
			return Base64.getUrlEncoder()
					.withoutPadding()
					.encodeToString(json);
		} catch (JsonProcessingException ex) {
			throw new GovernanceTimelineCursorDecodeException();
		}
	}

	@Override
	public GovernanceTimelineCursor decode(String encodedCursor) {
		if (encodedCursor == null || encodedCursor.isBlank()) {
			throw new GovernanceTimelineCursorDecodeException();
		}

		try {
			byte[] decoded = Base64.getUrlDecoder().decode(
					encodedCursor.getBytes(StandardCharsets.UTF_8)
			);
			GovernanceTimelineCursor cursor = objectMapper.readValue(
					decoded,
					GovernanceTimelineCursor.class
			);
			validate(cursor);
			return cursor;
		} catch (IllegalArgumentException | IOException ex) {
			throw new GovernanceTimelineCursorDecodeException();
		}
	}

	private void validate(GovernanceTimelineCursor cursor) {
		if (cursor == null
				|| cursor.occurredAt() == null
				|| cursor.eventType() == null
				|| cursor.eventType().isBlank()
				|| cursor.eventId() == null
				|| cursor.eventId().isBlank()) {
			throw new GovernanceTimelineCursorDecodeException();
		}
	}
}
