package com.fintech.sre.agent.admin.knowledge;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.admin.knowledge.audit.KnowledgeIngestionAuditIdGenerator;
import com.fintech.sre.agent.admin.knowledge.audit.KnowledgeIngestionAuditLog;
import com.fintech.sre.agent.admin.knowledge.audit.KnowledgeIngestionAuditLogger;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingPreparationResult;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingResult;
import com.fintech.sre.agent.knowledge.embedding.KnowledgeEmbeddingPreparationPipeline;
import com.fintech.sre.agent.knowledge.vector.upsert.KnowledgeVectorIngestionPipeline;
import com.fintech.sre.agent.knowledge.vector.upsert.KnowledgeVectorIngestionResult;
import com.fintech.sre.agent.knowledge.vector.upsert.VectorUpsertResult;

import reactor.core.publisher.Mono;

@Service
public class KnowledgeIngestionAdminService {

	private final KnowledgeIngestionPathValidator pathValidator;
	private final KnowledgeEmbeddingPreparationPipeline preparationPipeline;
	private final KnowledgeVectorIngestionPipeline vectorIngestionPipeline;
	private final KnowledgeIngestionAuditLogger auditLogger;
	private final KnowledgeIngestionAuditIdGenerator auditIdGenerator;

	public KnowledgeIngestionAdminService(
			KnowledgeIngestionPathValidator pathValidator,
			KnowledgeEmbeddingPreparationPipeline preparationPipeline,
			KnowledgeVectorIngestionPipeline vectorIngestionPipeline,
			KnowledgeIngestionAuditLogger auditLogger,
			KnowledgeIngestionAuditIdGenerator auditIdGenerator
	) {
		this.pathValidator = pathValidator;
		this.preparationPipeline = preparationPipeline;
		this.vectorIngestionPipeline = vectorIngestionPipeline;
		this.auditLogger = auditLogger;
		this.auditIdGenerator = auditIdGenerator;
	}

	public Mono<KnowledgeIngestionAdminResponse> ingest(KnowledgeIngestionAdminRequest request) {
		return Mono.defer(() -> {
			String auditId = auditIdGenerator.generate();
			Path rootPath = pathValidator.validate(request.portfolioRootPath());

			if (request.dryRun()) {
				return preparationPipeline.prepare(rootPath)
						.map(preparation -> toDryRunResponse(auditId, request, preparation));
			}

			return vectorIngestionPipeline.ingest(rootPath)
					.map(result -> toIngestResponse(auditId, request, result));
		}).onErrorResume(KnowledgeIngestionRejectedException.class, Mono::error)
				.onErrorResume(ex -> Mono.just(toFailureResponse(request, ex)))
				.flatMap(response -> audit(response, request)
						.onErrorResume(ex -> Mono.empty())
						.thenReturn(response));
	}

	private KnowledgeIngestionAdminResponse toDryRunResponse(
			String auditId,
			KnowledgeIngestionAdminRequest request,
			EmbeddingPreparationResult preparation
	) {
		return new KnowledgeIngestionAdminResponse(
				auditId,
				"DRY_RUN_COMPLETED",
				true,
				preparation == null ? 0 : preparation.requests().size(),
				0,
				0,
				0,
				0,
				preparation == null ? List.of() : preparation.rejectedChunkIds(),
				preparation == null ? List.of() : preparation.errors()
		);
	}

	private KnowledgeIngestionAdminResponse toIngestResponse(
			String auditId,
			KnowledgeIngestionAdminRequest request,
			KnowledgeVectorIngestionResult result
	) {
		EmbeddingPreparationResult preparation = result == null ? null : result.preparationResult();
		EmbeddingResult embedding = result == null ? null : result.embeddingResult();
		VectorUpsertResult upsert = result == null ? null : result.upsertResult();

		return new KnowledgeIngestionAdminResponse(
				auditId,
				"INGEST_COMPLETED",
				false,
				preparation == null ? 0 : preparation.requests().size(),
				embedding == null ? 0 : embedding.vectors().size(),
				embedding == null ? 0 : embedding.failures().size(),
				upsert == null ? 0 : upsert.upsertedPointIds().size(),
				upsert == null ? 0 : upsert.failures().size(),
				preparation == null ? List.of() : preparation.rejectedChunkIds(),
				collectErrors(preparation, embedding, upsert)
		);
	}

	private KnowledgeIngestionAdminResponse toFailureResponse(
			KnowledgeIngestionAdminRequest request,
			Throwable throwable
	) {
		String auditId = auditIdGenerator.generate();
		String message = throwable == null || throwable.getMessage() == null
				? "Knowledge ingestion failed."
				: throwable.getMessage();

		return new KnowledgeIngestionAdminResponse(
				auditId,
				"INGEST_FAILED",
				request != null && request.dryRun(),
				0,
				0,
				0,
				0,
				0,
				List.of(),
				List.of(message)
		);
	}

	private Mono<Void> audit(
			KnowledgeIngestionAdminResponse response,
			KnowledgeIngestionAdminRequest request
	) {
		return auditLogger.log(new KnowledgeIngestionAuditLog(
				response.auditId(),
				Instant.now(),
				safe(request == null ? null : request.requestedBy()),
				safe(request == null ? null : request.reason()),
				safe(request == null ? null : request.portfolioRootPath()),
				response.dryRun(),
				response.status(),
				response.preparedEmbeddingRequests(),
				response.embeddedVectors(),
				response.embeddingFailures(),
				response.upsertedPoints(),
				response.upsertFailures(),
				response.rejectedChunkIds(),
				response.errors()
		));
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private List<String> collectErrors(
			EmbeddingPreparationResult preparation,
			EmbeddingResult embedding,
			VectorUpsertResult upsert
	) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<>();

		if (preparation != null && preparation.errors() != null) {
			errors.addAll(preparation.errors());
		}

		if (embedding != null && embedding.failures() != null) {
			embedding.failures().forEach(failure ->
					errors.add(failure.chunkId() + ": " + failure.reasonCode() + " - " + failure.reason())
			);
		}

		if (upsert != null && upsert.failures() != null) {
			upsert.failures().forEach(failure ->
					errors.add(failure.pointId() + ": " + failure.reasonCode() + " - " + failure.reason())
			);
		}

		return List.copyOf(errors);
	}
}
