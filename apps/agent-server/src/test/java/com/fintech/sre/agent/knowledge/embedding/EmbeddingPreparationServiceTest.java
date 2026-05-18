package com.fintech.sre.agent.knowledge.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;
import com.fintech.sre.agent.knowledge.chunk.KnowledgeChunk;
import com.fintech.sre.agent.knowledge.chunk.KnowledgeChunkValidator;

class EmbeddingPreparationServiceTest {

	private final EmbeddingPreparationService service =
			new EmbeddingPreparationService(new KnowledgeChunkValidator());

	@Test
	void actionableChunkMustPreserveScenarioIds() {
		KnowledgeChunk chunk = new KnowledgeChunk(
				"runbook/payment#chunk-0",
				"runbook/payment",
				KnowledgeDocumentType.RUNBOOK,
				"Payment Runbook",
				"runbooks/payment.md",
				"payment",
				"payment-api",
				0,
				"content",
				"summary",
				List.of(),
				List.of("runbook/payment"),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of("LATENCY_SPIKE"),
				List.of("RATE_LIMIT"),
				Map.of()
		);

		EmbeddingPreparationResult result = service.prepare(List.of(chunk));

		assertThat(result.requests()).isEmpty();
		assertThat(result.rejectedChunkIds()).contains("runbook/payment#chunk-0");
	}
}
