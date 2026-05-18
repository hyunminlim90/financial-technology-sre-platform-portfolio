package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DefaultGovernanceTimelineCursorCodecTest {

	private final GovernanceTimelineCursorCodec codec =
			new DefaultGovernanceTimelineCursorCodec(objectMapper());

	@Test
	void shouldEncodeAndDecodeRoundTrip() {
		GovernanceTimelineCursor cursor = new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T00:00:00Z"),
				"RECOMMENDATION_CREATED",
				"RECOMMENDATION_RECORD:rec-1"
		);

		String encoded = codec.encode(cursor);
		GovernanceTimelineCursor decoded = codec.decode(encoded);

		assertThat(decoded).isEqualTo(cursor);
	}

	@Test
	void shouldCreateUrlSafeCursorWithoutPadding() {
		String encoded = codec.encode(new GovernanceTimelineCursor(
				Instant.parse("2026-05-14T00:00:00Z"),
				"RECOMMENDATION_CREATED",
				"RECOMMENDATION_RECORD:rec-1"
		));

		assertThat(encoded).doesNotContain("=");
		assertThat(encoded).doesNotContain("+");
		assertThat(encoded).doesNotContain("/");
	}

	@Test
	void shouldRejectInvalidOrTamperedCursor() {
		assertThatThrownBy(() -> codec.decode("%%%"))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldRejectBlankCursor() {
		assertThatThrownBy(() -> codec.decode("   "))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldRejectMissingOccurredAt() {
		String encoded = encodeJson(
				"{\"eventType\":\"RECOMMENDATION_CREATED\",\"eventId\":\"RECOMMENDATION_RECORD:rec-1\"}"
		);

		assertThatThrownBy(() -> codec.decode(encoded))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldRejectMissingEventType() {
		String encoded = encodeJson(
				"{\"occurredAt\":\"2026-05-14T00:00:00Z\",\"eventId\":\"RECOMMENDATION_RECORD:rec-1\"}"
		);

		assertThatThrownBy(() -> codec.decode(encoded))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldRejectMissingEventId() {
		String encoded = encodeJson(
				"{\"occurredAt\":\"2026-05-14T00:00:00Z\",\"eventType\":\"RECOMMENDATION_CREATED\"}"
		);

		assertThatThrownBy(() -> codec.decode(encoded))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldRejectBlankEventId() {
		String encoded = encodeJson(
				"{\"occurredAt\":\"2026-05-14T00:00:00Z\",\"eventType\":\"RECOMMENDATION_CREATED\",\"eventId\":\"   \"}"
		);

		assertThatThrownBy(() -> codec.decode(encoded))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.hasMessage("Invalid timeline cursor.");
	}

	@Test
	void shouldNotExposeRawCursorInExceptionMessage() {
		String raw = "%%%sensitive%%%";

		assertThatThrownBy(() -> codec.decode(raw))
				.isInstanceOf(GovernanceTimelineCursorDecodeException.class)
				.satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(raw));
	}

	private String encodeJson(String json) {
		return java.util.Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
