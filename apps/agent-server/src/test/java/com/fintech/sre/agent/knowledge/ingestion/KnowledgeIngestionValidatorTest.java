package com.fintech.sre.agent.knowledge.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

class KnowledgeIngestionValidatorTest {

	private final KnowledgeIngestionValidator validator = new KnowledgeIngestionValidator();

	@Test
	void runbookMustHaveScenarioRunbookEvidenceAndAction() {
		KnowledgeIngestionDocument document = base(KnowledgeDocumentType.RUNBOOK);

		KnowledgeIngestionValidationResult result = validator.validate(document);

		assertThat(result.valid()).isTrue();
	}

	@Test
	void ragDocMustNotDefineActionTypes() {
		KnowledgeIngestionDocument document = new KnowledgeIngestionDocument(
				"rag/payment-overview",
				KnowledgeDocumentType.RAG_DOC,
				"Payment Overview",
				"rag/docs/payment-overview.md",
				"payment",
				"payment-api",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of("SCALE_OUT"),
				"overview",
				"content",
				Map.of()
		);

		KnowledgeIngestionValidationResult result = validator.validate(document);

		assertThat(result.valid()).isFalse();
		assertThat(result.errors()).anyMatch(error -> error.contains("RAG_DOC must not define actionTypes"));
	}

	@Test
	void scenarioMustNotBeActionable() {
		KnowledgeIngestionDocument document = new KnowledgeIngestionDocument(
				"scenario/payment-latency-spike",
				KnowledgeDocumentType.SCENARIO,
				"Payment Latency Spike",
				"scenarios/payment-latency-spike.md",
				"payment",
				"payment-api",
				List.of("scenario/payment-latency-spike"),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of("LATENCY_SPIKE"),
				List.of("RATE_LIMIT"),
				"scenario",
				"content",
				Map.of()
		);

		KnowledgeIngestionValidationResult result = validator.validate(document);

		assertThat(result.valid()).isFalse();
		assertThat(result.errors()).anyMatch(error -> error.contains("Only RUNBOOK or POLICY documents may define actionTypes"));
	}

	private KnowledgeIngestionDocument base(KnowledgeDocumentType type) {
		return new KnowledgeIngestionDocument(
				"runbook/payment-latency-mitigation",
				type,
				"Payment Latency Mitigation",
				"runbooks/payment-latency-mitigation.md",
				"payment",
				"payment-api",
				List.of("scenario/payment-latency-spike"),
				List.of("runbook/payment-latency-mitigation"),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of("LATENCY_SPIKE", "ERROR_RATE_SPIKE"),
				List.of("RATE_LIMIT"),
				"Mitigate payment latency safely.",
				"content",
				Map.of("owner", "sre")
		);
	}
}
