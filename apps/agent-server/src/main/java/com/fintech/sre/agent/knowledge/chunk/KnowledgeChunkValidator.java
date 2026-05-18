package com.fintech.sre.agent.knowledge.chunk;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

@Component
public class KnowledgeChunkValidator {

	public List<String> validate(KnowledgeChunk chunk) {
		List<String> errors = new ArrayList<>();

		if (chunk == null) {
			return List.of("chunk must not be null");
		}

		require(errors, chunk.id(), "id");
		require(errors, chunk.documentId(), "documentId");
		require(errors, chunk.documentType(), "documentType");
		require(errors, chunk.path(), "path");
		require(errors, chunk.domain(), "domain");
		require(errors, chunk.service(), "service");
		require(errors, chunk.content(), "content");

		if (chunk.documentType() == KnowledgeDocumentType.RAG_DOC
				&& chunk.actionTypes() != null
				&& !chunk.actionTypes().isEmpty()) {
			errors.add("RAG_DOC chunk must not define actionTypes");
		}

		if (chunk.actionTypes() != null
				&& !chunk.actionTypes().isEmpty()
				&& (chunk.scenarioIds() == null || chunk.scenarioIds().isEmpty())) {
			errors.add("Actionable chunk must preserve scenarioIds. No Scenario -> No Action.");
		}

		return errors;
	}

	private void require(List<String> errors, Object value, String field) {
		if (value == null) {
			errors.add(field + " is required");
			return;
		}

		if (value instanceof String text && text.isBlank()) {
			errors.add(field + " is required");
		}
	}
}
