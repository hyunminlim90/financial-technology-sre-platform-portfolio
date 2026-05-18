package com.fintech.sre.agent.knowledge.ingestion;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeIngestionPayloadMapper {

	public Map<String, Object> toPayload(KnowledgeIngestionDocument document) {
		Map<String, Object> payload = new LinkedHashMap<>();

		payload.put("id", document.id());
		payload.put("type", document.type().name());
		payload.put("title", document.title());
		payload.put("path", document.path());
		payload.put("domain", document.domain());
		payload.put("service", document.service());

		payload.put("scenarioIds", document.scenarioIds());
		payload.put("runbookIds", document.runbookIds());
		payload.put("postmortemIds", document.postmortemIds());
		payload.put("improvementIds", document.improvementIds());
		payload.put("preventiveDesignIds", document.preventiveDesignIds());
		payload.put("policyIds", document.policyIds());

		payload.put("evidenceCodes", document.evidenceCodes());
		payload.put("actionTypes", document.actionTypes());

		payload.put("summary", document.summary());
		payload.put("content", document.content());
		payload.put("metadata", document.metadata());

		return payload;
	}
}
