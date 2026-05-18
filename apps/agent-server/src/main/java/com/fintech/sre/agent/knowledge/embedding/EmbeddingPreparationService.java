package com.fintech.sre.agent.knowledge.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.knowledge.chunk.KnowledgeChunk;
import com.fintech.sre.agent.knowledge.chunk.KnowledgeChunkValidator;

@Service
public class EmbeddingPreparationService {

	private final KnowledgeChunkValidator chunkValidator;

	public EmbeddingPreparationService(KnowledgeChunkValidator chunkValidator) {
		this.chunkValidator = chunkValidator;
	}

	public EmbeddingPreparationResult prepare(List<KnowledgeChunk> chunks) {
		List<EmbeddingRequest> requests = new ArrayList<>();
		List<String> rejectedChunkIds = new ArrayList<>();
		List<String> errors = new ArrayList<>();

		if (chunks == null || chunks.isEmpty()) {
			return new EmbeddingPreparationResult(List.of(), List.of(), List.of());
		}

		for (KnowledgeChunk chunk : chunks) {
			List<String> validationErrors = chunkValidator.validate(chunk);

			if (!validationErrors.isEmpty()) {
				rejectedChunkIds.add(chunk == null ? "unknown" : chunk.id());
				errors.addAll(validationErrors);
				continue;
			}

			requests.add(toEmbeddingRequest(chunk));
		}

		return new EmbeddingPreparationResult(
				List.copyOf(requests),
				List.copyOf(rejectedChunkIds),
				List.copyOf(errors)
		);
	}

	private EmbeddingRequest toEmbeddingRequest(KnowledgeChunk chunk) {
		return new EmbeddingRequest(
				chunk.id(),
				chunk.documentId(),
				chunk.content(),
				toPayload(chunk),
				List.of(
						chunk.documentType().name(),
						chunk.domain(),
						chunk.service()
				)
		);
	}

	private Map<String, Object> toPayload(KnowledgeChunk chunk) {
		Map<String, Object> payload = new java.util.LinkedHashMap<>();

		payload.put("id", chunk.id());
		payload.put("documentId", chunk.documentId());
		payload.put("type", chunk.documentType().name());
		payload.put("title", chunk.title());
		payload.put("path", chunk.path());
		payload.put("domain", chunk.domain());
		payload.put("service", chunk.service());
		payload.put("chunkIndex", chunk.chunkIndex());

		payload.put("scenarioIds", chunk.scenarioIds());
		payload.put("runbookIds", chunk.runbookIds());
		payload.put("postmortemIds", chunk.postmortemIds());
		payload.put("improvementIds", chunk.improvementIds());
		payload.put("preventiveDesignIds", chunk.preventiveDesignIds());
		payload.put("policyIds", chunk.policyIds());
		payload.put("evidenceCodes", chunk.evidenceCodes());
		payload.put("actionTypes", chunk.actionTypes());

		payload.put("summary", chunk.summary());
		payload.put("content", chunk.content());
		payload.put("metadata", chunk.metadata());

		return payload;
	}
}
