package com.fintech.sre.agent.alert.dedup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class InMemoryAlertDeduplicationStoreTest {

	@Test
	void shouldDetectDuplicateWithinWindow() {
		AlertDeduplicationProperties properties =
				new AlertDeduplicationProperties(true, Duration.ofMinutes(5));

		InMemoryAlertDeduplicationStore store =
				new InMemoryAlertDeduplicationStore(properties);

		AlertDeduplicationKey key =
				AlertDeduplicationKey.of("HighP99Latency", "payment-api", "firing");

		AlertDeduplicationResult first = store.checkAndRecord(key).block();
		AlertDeduplicationResult second = store.checkAndRecord(key).block();

		assertThat(first.duplicate()).isFalse();
		assertThat(second.duplicate()).isTrue();
		assertThat(second.occurrenceCount()).isEqualTo(2);
	}

	@Test
	void shouldNotDeduplicateWhenDisabled() {
		AlertDeduplicationProperties properties =
				new AlertDeduplicationProperties(false, Duration.ofMinutes(5));

		InMemoryAlertDeduplicationStore store =
				new InMemoryAlertDeduplicationStore(properties);

		AlertDeduplicationKey key =
				AlertDeduplicationKey.of("HighP99Latency", "payment-api", "firing");

		AlertDeduplicationResult first = store.checkAndRecord(key).block();
		AlertDeduplicationResult second = store.checkAndRecord(key).block();

		assertThat(first.duplicate()).isFalse();
		assertThat(second.duplicate()).isFalse();
	}
}
