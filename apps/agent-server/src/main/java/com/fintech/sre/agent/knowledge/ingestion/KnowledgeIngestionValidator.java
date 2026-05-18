package com.fintech.sre.agent.knowledge.ingestion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

@Component
public class KnowledgeIngestionValidator {

	public KnowledgeIngestionValidationResult validate(KnowledgeIngestionDocument document) {
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		if (document == null) {
			return KnowledgeIngestionValidationResult.invalid(List.of("document must not be null"));
		}

		require(errors, document.id(), "id");
		require(errors, document.type(), "type");
		require(errors, document.title(), "title");
		require(errors, document.path(), "path");
		require(errors, document.domain(), "domain");
		require(errors, document.service(), "service");
		require(errors, document.summary(), "summary");
		require(errors, document.content(), "content");

		validateByType(document, errors, warnings);
		validateActionSafety(document, errors, warnings);

		return KnowledgeIngestionValidationResult.of(errors, warnings);
	}

	private void validateByType(
			KnowledgeIngestionDocument document,
			List<String> errors,
			List<String> warnings
	) {
		if (document.type() == KnowledgeDocumentType.SCENARIO) {
			requireAny(errors, document.scenarioIds(), "scenarioIds");
			warnIfAnyAction(document, warnings, "SCENARIO should describe detection/matching, not executable action");
		}

		if (document.type() == KnowledgeDocumentType.RUNBOOK) {
			requireAny(errors, document.scenarioIds(), "scenarioIds");
			requireAny(errors, document.runbookIds(), "runbookIds");
			requireAny(errors, document.actionTypes(), "actionTypes");
			requireAny(errors, document.evidenceCodes(), "evidenceCodes");
		}

		if (document.type() == KnowledgeDocumentType.POSTMORTEM) {
			requireAny(errors, document.postmortemIds(), "postmortemIds");
			warnIfMissing(document.scenarioIds(), warnings, "POSTMORTEM should link scenarioIds when possible");
			warnIfMissing(document.runbookIds(), warnings, "POSTMORTEM should link runbookIds when possible");
		}

		if (document.type() == KnowledgeDocumentType.IMPROVEMENT) {
			requireAny(errors, document.improvementIds(), "improvementIds");
			warnIfMissing(document.postmortemIds(), warnings, "IMPROVEMENT should link postmortemIds when possible");
		}

		if (document.type() == KnowledgeDocumentType.PREVENTIVE_DESIGN) {
			requireAny(errors, document.preventiveDesignIds(), "preventiveDesignIds");
			warnIfMissing(document.improvementIds(), warnings, "PREVENTIVE_DESIGN should link improvementIds when possible");
		}

		if (document.type() == KnowledgeDocumentType.POLICY) {
			requireAny(errors, document.policyIds(), "policyIds");
		}

		if (document.type() == KnowledgeDocumentType.RAG_DOC) {
			if (hasAny(document.actionTypes())) {
				errors.add("RAG_DOC must not define actionTypes; rag/docs cannot become actionable knowledge directly");
			}
			warnings.add("RAG_DOC is auxiliary knowledge only and must not decide ActionCommand");
		}
	}

	private void validateActionSafety(
			KnowledgeIngestionDocument document,
			List<String> errors,
			List<String> warnings
	) {
		if (!hasAny(document.actionTypes())) {
			return;
		}

		if (document.type() != KnowledgeDocumentType.RUNBOOK
				&& document.type() != KnowledgeDocumentType.POLICY) {
			errors.add("Only RUNBOOK or POLICY documents may define actionTypes");
		}

		if (!hasAny(document.scenarioIds())) {
			errors.add("Actionable document must link scenarioIds. No Scenario → No Action.");
		}

		if (!hasAny(document.evidenceCodes())) {
			warnings.add("Actionable document should include evidenceCodes for evidence-based recommendation.");
		}
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

	private void requireAny(List<String> errors, List<String> values, String field) {
		if (!hasAny(values)) {
			errors.add(field + " must not be empty");
		}
	}

	private void warnIfMissing(List<String> values, List<String> warnings, String message) {
		if (!hasAny(values)) {
			warnings.add(message);
		}
	}

	private void warnIfAnyAction(
			KnowledgeIngestionDocument document,
			List<String> warnings,
			String message
	) {
		if (hasAny(document.actionTypes())) {
			warnings.add(message);
		}
	}

	private boolean hasAny(List<String> values) {
		return values != null && !values.isEmpty();
	}
}
