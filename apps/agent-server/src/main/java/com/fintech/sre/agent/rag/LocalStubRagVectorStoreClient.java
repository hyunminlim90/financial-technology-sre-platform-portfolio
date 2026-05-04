package com.fintech.sre.agent.rag;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class LocalStubRagVectorStoreClient implements RagVectorStoreClient {

	@Override
	public Mono<List<RagDocument>> search(RagSearchQuery query) {
		List<RagDocument> documents = List.of(
				document(
						"protocol-payment-ops",
						"/protocols/payment-ops",
						"chunk-001",
						"Payment Operations Protocol",
						KnowledgeType.PROTOCOL,
						"Production payment operations require explicit rollback, approval, and bounded changes.",
						metadata(
								"Payment Operations Protocol",
								KnowledgeType.PROTOCOL,
								"PAYMENTS",
								"LATENCY_AND_TIMEOUT",
								query.environment(),
								query.severityHint(),
								query.impactScopeHint(),
								List.of(query.service()),
								List.of("protocol", "approval"),
								List.of("scenario-checkout-timeout"),
								List.of("runbook-api-latency"),
								List.of(),
								List.of(),
								List.of("preventive-idempotency"),
								Map.of("source", "stub")
						),
						0.84
				),
				document(
						"scenario-checkout-timeout",
						"/scenarios/checkout-timeout",
						"chunk-010",
						"Checkout Timeout Escalation",
						KnowledgeType.SCENARIO,
						"Timeout and retry amplification on checkout path.",
						metadata(
								"Checkout Timeout Escalation",
								KnowledgeType.SCENARIO,
								"PAYMENTS",
								"LATENCY_AND_TIMEOUT",
								query.environment(),
								"SEV_2",
								"PARTIAL",
								List.of("checkout-service", "payment-api"),
								List.of("timeout", "retry", "checkout"),
								List.of(),
								List.of("runbook-api-latency"),
								List.of("postmortem-checkout-queue"),
								List.of("improvement-retry-db"),
								List.of("preventive-idempotency"),
								Map.of("source", "stub")
						),
						0.93
				),
				document(
						"runbook-api-latency",
						"/runbooks/api-latency",
						"chunk-020",
						"API Latency Runbook",
						KnowledgeType.RUNBOOK,
						"Scale conservatively, inspect downstream saturation, and apply targeted rate limiting.",
						metadata(
								"API Latency Runbook",
								KnowledgeType.RUNBOOK,
								"PAYMENTS",
								"LATENCY_AND_TIMEOUT",
								query.environment(),
								"SEV_2",
								"PARTIAL",
								List.of("checkout-service", "payment-api"),
								List.of("runbook", "latency"),
								List.of("scenario-checkout-timeout"),
								List.of(),
								List.of("postmortem-checkout-queue"),
								List.of("improvement-retry-db"),
								List.of(),
								Map.of("source", "stub")
						),
						0.95
				),
				document(
						"improvement-retry-db",
						"/improvements/retry-db",
						"chunk-030",
						"Retry Storm with DB Pending Constraint",
						KnowledgeType.IMPROVEMENT,
						"Avoid aggressive scale-out when retry rate and DB pending are both elevated.",
						metadata(
								"Retry Storm with DB Pending Constraint",
								KnowledgeType.IMPROVEMENT,
								"PAYMENTS",
								"LATENCY_AND_TIMEOUT",
								query.environment(),
								"SEV_2",
								"PARTIAL",
								List.of("checkout-service"),
								List.of("retry", "db"),
								List.of("scenario-checkout-timeout"),
								List.of("runbook-api-latency"),
								List.of(),
								List.of(),
								List.of(),
								Map.of("source", "stub")
						),
						0.90
				),
				document(
						"preventive-idempotency",
						"/preventive-designs/idempotency",
						"chunk-040",
						"Idempotency Protection for Payment Paths",
						KnowledgeType.PREVENTIVE_DESIGN,
						"Payment and checkout paths should preserve idempotency and duplicate protection.",
						metadata(
								"Idempotency Protection for Payment Paths",
								KnowledgeType.PREVENTIVE_DESIGN,
								"PAYMENTS",
								"DUPLICATE_PAYMENT_RISK",
								query.environment(),
								"SEV_2",
								"PARTIAL",
								List.of("payment-api", "checkout-service"),
								List.of("idempotency", "payment"),
								List.of("scenario-checkout-timeout"),
								List.of("runbook-api-latency"),
								List.of(),
								List.of(),
								List.of(),
								Map.of("source", "stub")
						),
						0.87
				),
				document(
						"postmortem-checkout-queue",
						"/postmortems/checkout-queue",
						"chunk-050",
						"Checkout Queue Saturation Postmortem",
						KnowledgeType.POSTMORTEM,
						"High error rate caused by queue backpressure and retry storm.",
						metadata(
								"Checkout Queue Saturation Postmortem",
								KnowledgeType.POSTMORTEM,
								"PAYMENTS",
								"LATENCY_AND_TIMEOUT",
								query.environment(),
								"SEV_2",
								"PARTIAL",
								List.of("checkout-service"),
								List.of("queue", "retry"),
								List.of("scenario-checkout-timeout"),
								List.of("runbook-api-latency"),
								List.of(),
								List.of(),
								List.of(),
								Map.of("source", "stub")
						),
						0.88
				),
				document(
						"rag-payment-retry",
						"/docs/payment-retry",
						"chunk-060",
						"Payment Retry Deep Diagnosis",
						KnowledgeType.RAG_DOC,
						"Retries must be bounded and protected by idempotency keys.",
						metadata(
								"Payment Retry Deep Diagnosis",
								KnowledgeType.RAG_DOC,
								"PAYMENTS",
								"LATENCY_AND_TIMEOUT",
								query.environment(),
								query.severityHint(),
								query.impactScopeHint(),
								List.of("payment-api"),
								List.of("retry", "diagnosis"),
								List.of("scenario-checkout-timeout"),
								List.of("runbook-api-latency"),
								List.of("postmortem-checkout-queue"),
								List.of("improvement-retry-db"),
								List.of("preventive-idempotency"),
								Map.of("source", "stub")
						),
						0.83
				)
		);
		return Mono.just(documents.stream()
				.filter(doc -> query.targetKnowledgeTypes() == null || query.targetKnowledgeTypes().contains(doc.knowledgeType()))
				.toList());
	}

	private RagDocument document(
			String documentId,
			String path,
			String chunkId,
			String title,
			KnowledgeType knowledgeType,
			String content,
			DocumentMetadata metadata,
			double score
	) {
		return new RagDocument(documentId, path, chunkId, title, knowledgeType, content, metadata, score);
	}

	private DocumentMetadata metadata(
			String title,
			KnowledgeType knowledgeType,
			String domain,
			String failureMode,
			String environment,
			String severity,
			String impactScope,
			List<String> services,
			List<String> tags,
			List<String> relatedScenarios,
			List<String> relatedRunbooks,
			List<String> relatedPostmortems,
			List<String> relatedImprovements,
			List<String> relatedPreventiveDesigns,
			Map<String, String> raw
	) {
		Instant now = Instant.now();
		return new DocumentMetadata(
				title,
				knowledgeType,
				domain,
				failureMode,
				environment,
				severity,
				impactScope,
				services,
				tags,
				relatedScenarios,
				relatedRunbooks,
				relatedPostmortems,
				relatedImprovements,
				relatedPreventiveDesigns,
				now.minusSeconds(86_400),
				now,
				raw
		);
	}
}
