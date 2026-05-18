package com.fintech.sre.agent.alert.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.alert.summary.AlertBatchSummary;

class InMemoryAlertIngestionAuditLoggerTest {

	private final InMemoryAlertIngestionAuditLogger logger =
			new InMemoryAlertIngestionAuditLogger();

	@Test
	void shouldStoreAlertIngestionAuditLog() {
		AlertIngestionAuditLog log = new AlertIngestionAuditLog(
				"audit-1",
				Instant.now(),
				"PROMETHEUS_ALERTMANAGER",
				"alert-1",
				"HighP99Latency",
				"firing",
				"CRITICAL",
				"payment-api",
				"payment",
				"sre-agent",
				1,
				0,
				List.of(),
				0,
				List.of(),
				new AlertBatchSummary(1, 1, 0, 0, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of()),
				List.of("incident-1"),
				List.of()
		);

		logger.log(log).block();

		assertThat(logger.logs()).hasSize(1);
		assertThat(logger.logs().get(0).auditId()).isEqualTo("audit-1");
	}
}
