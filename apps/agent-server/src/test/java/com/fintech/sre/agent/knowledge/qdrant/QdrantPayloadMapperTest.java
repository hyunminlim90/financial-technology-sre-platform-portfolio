package com.fintech.sre.agent.knowledge.qdrant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

class QdrantPayloadMapperTest {

	private final QdrantPayloadMapper mapper = new QdrantPayloadMapper();

	@Test
	void shouldMapPayloadToKnowledgeDocument() {
		var document = mapper.toDocument("point-1", Map.ofEntries(
				Map.entry("id", "doc-1"),
				Map.entry("type", "RUNBOOK"),
				Map.entry("title", "Payment Runbook"),
				Map.entry("path", "runbooks/payment.yaml"),
				Map.entry("domain", "payment"),
				Map.entry("service", "payment-service"),
				Map.entry("scenarioIds", List.of("scenario-1")),
				Map.entry("runbookIds", List.of("runbook-1")),
				Map.entry("evidenceCodes", List.of("ERROR_RATE_HIGH")),
				Map.entry("actionTypes", List.of("RATE_LIMIT")),
				Map.entry("score", 0.87d),
				Map.entry("summary", "Matched payment mitigation runbook.")
		));

		assertThat(document).isNotNull();
		assertThat(document.id()).isEqualTo("doc-1");
		assertThat(document.type()).isEqualTo(KnowledgeDocumentType.RUNBOOK);
		assertThat(document.actionTypes()).containsExactly("RATE_LIMIT");
		assertThat(document.score()).isEqualTo(0.87d);
	}

	@Test
	void shouldFallbackToRagDocWhenTypeIsUnknown() {
		var document = mapper.toDocument("point-2", Map.of(
				"type", "UNKNOWN_TYPE",
				"title", "Reference Doc"
		));

		assertThat(document.type()).isEqualTo(KnowledgeDocumentType.RAG_DOC);
		assertThat(document.id()).isEqualTo("point-2");
	}
}
