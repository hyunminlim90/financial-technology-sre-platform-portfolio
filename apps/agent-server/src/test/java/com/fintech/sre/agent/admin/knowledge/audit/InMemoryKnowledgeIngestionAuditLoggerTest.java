package com.fintech.sre.agent.admin.knowledge.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class InMemoryKnowledgeIngestionAuditLoggerTest {

	private final InMemoryKnowledgeIngestionAuditLogger logger =
			new InMemoryKnowledgeIngestionAuditLogger();

	@Test
	void shouldStoreAuditLog() {
		KnowledgeIngestionAuditLog log = new KnowledgeIngestionAuditLog(
				"audit-1",
				Instant.now(),
				"operator",
				"manual ingestion",
				"/tmp/portfolio",
				true,
				"DRY_RUN_COMPLETED",
				10,
				0,
				0,
				0,
				0,
				List.of(),
				List.of()
		);

		logger.log(log).block();

		assertThat(logger.logs()).hasSize(1);
		assertThat(logger.logs().get(0).auditId()).isEqualTo("audit-1");
	}
}
