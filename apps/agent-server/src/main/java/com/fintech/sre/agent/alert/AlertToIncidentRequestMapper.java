package com.fintech.sre.agent.alert;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.alert.evidence.AlertEvidenceMapper;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

@Component
public class AlertToIncidentRequestMapper {

	private final AlertEvidenceMapper evidenceMapper;

	public AlertToIncidentRequestMapper(AlertEvidenceMapper evidenceMapper) {
		this.evidenceMapper = evidenceMapper;
	}

	public IncidentRecommendationRequest toRequest(AlertEvent alert) {
		Map<String, String> labels = new LinkedHashMap<>();
		EvidenceSignal signal = evidenceMapper.toSignal(alert);

		labels.put("source", alert.source().name());
		labels.put("alertName", alert.alertName());
		labels.put("severity", alert.severity().name());
		labels.put("status", alert.status());
		labels.put("service", alert.service());
		labels.put("domain", alert.domain());
		labels.put("namespace", alert.namespace());
		labels.put("alertSource", alert.source().name());
		labels.put("alertStatus", alert.status());
		labels.put("alertSeverity", alert.severity().name());
		labels.put("evidenceCode", signal.code());
		labels.put("evidenceSource", signal.source().name());
		labels.put("evidenceSeverity", signal.severity().name());
		labels.put("evidenceSignalId", signal.id());

		if (alert.labels() != null) {
			labels.putAll(alert.labels());
		}

		String description = alert.description();
		if (description == null || description.isBlank()) {
			description = signal.summary();
		}

		return new IncidentRecommendationRequest(
				alert.alertId(),
				alert.alertName(),
				alert.service(),
				alert.namespace(),
				alert.severity().name(),
				alert.startsAt(),
				labels,
				null,
				java.util.List.of(),
				java.util.List.of(),
				null,
				description
		);
	}
}
