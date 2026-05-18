package com.fintech.sre.agent.knowledge.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;
import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionValidator;

class KnowledgeScanServiceTest {

	private final KnowledgeScanService service = new KnowledgeScanService(
			new FileSystemKnowledgeSourceScanner(new SimpleMarkdownMetadataParser()),
			new KnowledgeDocumentFactory(),
			new KnowledgeIngestionValidator()
	);

	@Test
	void runbookMarkdownShouldBecomeValidIngestionDocument(@TempDir Path tempDir) throws Exception {
		Path runbook = tempDir.resolve("runbooks/payment-latency.md");
		Files.createDirectories(runbook.getParent());
		Files.writeString(runbook, """
				---
				id: runbook/payment-latency-mitigation
				title: Payment Latency Mitigation
				domain: payment
				service: payment-api
				scenarioIds:
				  - scenario/payment-latency-spike
				runbookIds:
				  - runbook/payment-latency-mitigation
				evidenceCodes:
				  - LATENCY_SPIKE
				actionTypes:
				  - RATE_LIMIT
				summary: Mitigate payment latency spike safely.
				---
				
				# Payment Latency Mitigation
				
				Use rate limiting first.
				""");

		KnowledgeScanResult result = service.scan(tempDir).block();

		assertThat(result).isNotNull();
		assertThat(result.validDocuments()).hasSize(1);
		assertThat(result.rejectedDocuments()).isEmpty();
		assertThat(result.validDocuments().get(0).type()).isEqualTo(KnowledgeDocumentType.RUNBOOK);
	}

	@Test
	void ragDocWithActionTypesShouldBeQuarantined(@TempDir Path tempDir) throws Exception {
		Path ragDoc = tempDir.resolve("rag/docs/payment-overview.md");
		Files.createDirectories(ragDoc.getParent());
		Files.writeString(ragDoc, """
				---
				id: rag/payment-overview
				title: Payment Overview
				domain: payment
				service: payment-api
				actionTypes:
				  - SCALE_OUT
				summary: overview
				---
				
				# Payment Overview
				
				content
				""");

		KnowledgeScanResult result = service.scan(tempDir).block();

		assertThat(result.validDocuments()).isEmpty();
		assertThat(result.rejectedDocuments()).hasSize(1);
		assertThat(result.rejectedDocuments().get(0).errors())
				.anyMatch(error -> error.contains("RAG_DOC must not define actionTypes"));
	}

	@Test
	void missingScenarioIdsShouldBeRejected(@TempDir Path tempDir) throws Exception {
		Path runbook = tempDir.resolve("runbooks/payment-latency.md");
		Files.createDirectories(runbook.getParent());
		Files.writeString(runbook, """
				---
				id: runbook/payment-latency-mitigation
				title: Payment Latency Mitigation
				domain: payment
				service: payment-api
				runbookIds:
				  - runbook/payment-latency-mitigation
				evidenceCodes:
				  - LATENCY_SPIKE
				actionTypes:
				  - RATE_LIMIT
				summary: Mitigate payment latency spike safely.
				---
				
				# Payment Latency Mitigation
				
				content
				""");

		KnowledgeScanResult result = service.scan(tempDir).block();

		assertThat(result.validDocuments()).isEmpty();
		assertThat(result.rejectedDocuments()).hasSize(1);
		assertThat(result.rejectedDocuments().get(0).errors())
				.anyMatch(error -> error.contains("scenarioIds"));
	}
}
