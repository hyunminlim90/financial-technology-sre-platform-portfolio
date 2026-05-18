package com.fintech.sre.agent.evidence;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

@Component
public class RequestEvidenceSignalExtractor {

	public List<EvidenceSignal> extract(IncidentRecommendationRequest request) {
		if (request == null || request.labels() == null) {
			return List.of();
		}

		Map<String, String> labels = request.labels();

		String code = labels.get("evidenceCode");
		if (code == null || code.isBlank()) {
			return List.of();
		}

		return List.of(new EvidenceSignal(
				valueOrDefault(labels.get("evidenceSignalId"), "request-evidence:" + request.incidentId()),
				EvidenceLayer.OBSERVABILITY,
				toSource(labels.get("evidenceSource")),
				toSeverity(labels.get("evidenceSeverity")),
				code,
				valueOrDefault(request.operatorNote(), "Evidence extracted from alert labels."),
				valueOrDefault(labels.get("observedValue"), "alert=" + labels.get("alertName")),
				valueOrDefault(labels.get("expectedValue"), "expected condition restored"),
				request.incidentId()
		));
	}

	private EvidenceSource toSource(String value) {
		if (value == null || value.isBlank()) {
			return EvidenceSource.MANUAL_INPUT;
		}

		try {
			return EvidenceSource.valueOf(value);
		} catch (IllegalArgumentException ex) {
			return EvidenceSource.MANUAL_INPUT;
		}
	}

	private EvidenceSeverity toSeverity(String value) {
		if (value == null || value.isBlank()) {
			return EvidenceSeverity.WARNING;
		}

		try {
			return EvidenceSeverity.valueOf(value);
		} catch (IllegalArgumentException ex) {
			return EvidenceSeverity.WARNING;
		}
	}

	private String valueOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
