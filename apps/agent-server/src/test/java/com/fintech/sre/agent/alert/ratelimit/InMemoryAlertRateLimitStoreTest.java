package com.fintech.sre.agent.alert.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class InMemoryAlertRateLimitStoreTest {

	@Test
	void shouldLimitAfterBudgetExceeded() {
		AlertRateLimitProperties properties =
				new AlertRateLimitProperties(true, Duration.ofMinutes(1), 1);

		InMemoryAlertRateLimitStore store =
				new InMemoryAlertRateLimitStore(properties);

		AlertRateLimitKey key = AlertRateLimitKey.of("payment-api", "CRITICAL");

		AlertRateLimitResult first = store.checkAndConsume(key).block();
		AlertRateLimitResult second = store.checkAndConsume(key).block();

		assertThat(first.allowed()).isTrue();
		assertThat(second.allowed()).isFalse();
		assertThat(second.used()).isEqualTo(2);
		assertThat(second.limit()).isEqualTo(1);
	}

	@Test
	void shouldAllowAllWhenDisabled() {
		AlertRateLimitProperties properties =
				new AlertRateLimitProperties(false, Duration.ofMinutes(1), 1);

		InMemoryAlertRateLimitStore store =
				new InMemoryAlertRateLimitStore(properties);

		AlertRateLimitKey key = AlertRateLimitKey.of("payment-api", "CRITICAL");

		AlertRateLimitResult first = store.checkAndConsume(key).block();
		AlertRateLimitResult second = store.checkAndConsume(key).block();

		assertThat(first.allowed()).isTrue();
		assertThat(second.allowed()).isTrue();
	}
}
