package com.fintech.sre.agent.admin.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.admin.knowledge.audit.InMemoryKnowledgeIngestionAuditLogger;
import com.fintech.sre.agent.admin.knowledge.audit.KnowledgeIngestionAuditIdGenerator;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingPreparationResult;
import com.fintech.sre.agent.knowledge.embedding.KnowledgeEmbeddingPreparationPipeline;
import com.fintech.sre.agent.knowledge.vector.upsert.KnowledgeVectorIngestionPipeline;

import reactor.core.publisher.Mono;

class KnowledgeIngestionAdminServiceAuditTest {

	@Test
	void dryRunShouldWriteAuditLog() {
		KnowledgeAdminProperties properties = new KnowledgeAdminProperties(
				true,
				List.of("/tmp/portfolio")
		);
		KnowledgeIngestionPathValidator validator = new KnowledgeIngestionPathValidator(properties);
		InMemoryKnowledgeIngestionAuditLogger auditLogger = new InMemoryKnowledgeIngestionAuditLogger();
		KnowledgeIngestionAuditIdGenerator auditIdGenerator = new KnowledgeIngestionAuditIdGenerator();

		KnowledgeEmbeddingPreparationPipeline preparationPipeline = new KnowledgeEmbeddingPreparationPipeline(
				null,
				null,
				null
		) {
			@Override
			public Mono<EmbeddingPreparationResult> prepare(Path rootPath) {
				return Mono.just(new EmbeddingPreparationResult(
						List.of(),
						List.of("rejected-1"),
						List.of("warning-1")
				));
			}
		};

		KnowledgeVectorIngestionPipeline vectorIngestionPipeline = new KnowledgeVectorIngestionPipeline(
				null,
				null,
				null
		) {
		};

		KnowledgeIngestionAdminService service = new KnowledgeIngestionAdminService(
				validator,
				preparationPipeline,
				vectorIngestionPipeline,
				auditLogger,
				auditIdGenerator
		);

		KnowledgeIngestionAdminResponse response = service.ingest(new KnowledgeIngestionAdminRequest(
				"/tmp/portfolio/scenarios",
				"operator",
				"manual dry run",
				true
		)).block();

		assertThat(response).isNotNull();
		assertThat(response.auditId()).isNotBlank();
		assertThat(auditLogger.logs()).hasSize(1);
		assertThat(auditLogger.logs().get(0).requestedBy()).isEqualTo("operator");
		assertThat(auditLogger.logs().get(0).reason()).isEqualTo("manual dry run");
		assertThat(auditLogger.logs().get(0).portfolioRootPath()).isEqualTo("/tmp/portfolio/scenarios");
		assertThat(auditLogger.logs().get(0).status()).isEqualTo("DRY_RUN_COMPLETED");
	}
}
