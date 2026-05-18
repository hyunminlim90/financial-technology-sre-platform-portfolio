package com.fintech.sre.agent.alert.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlertEvidenceCodeResolverTest {

	private final AlertEvidenceCodeResolver resolver = new AlertEvidenceCodeResolver();

	@Test
	void shouldResolveLatencyAlert() {
		assertThat(resolver.resolve("HighP99Latency")).isEqualTo("LATENCY_SPIKE");
	}

	@Test
	void shouldResolveErrorAlert() {
		assertThat(resolver.resolve("High5xxErrorRate")).isEqualTo("ERROR_RATE_SPIKE");
	}

	@Test
	void shouldResolveConsumerLagAlert() {
		assertThat(resolver.resolve("KafkaConsumerLagHigh")).isEqualTo("CONSUMER_LAG_SPIKE");
	}
}
