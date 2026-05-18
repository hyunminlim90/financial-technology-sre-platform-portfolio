package com.fintech.sre.agent.alert.evidence;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.evidence.EvidenceLayer;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.evidence.EvidenceSource;

@Component
public class AlertEvidenceMapper {

	private final AlertEvidenceCodeResolver codeResolver;
	private final AlertEvidenceSeverityMapper severityMapper;

	public AlertEvidenceMapper(
			AlertEvidenceCodeResolver codeResolver,
			AlertEvidenceSeverityMapper severityMapper
	) {
		this.codeResolver = codeResolver;
		this.severityMapper = severityMapper;
	}

	public EvidenceSignal toSignal(AlertEvent alert) {
		String code = codeResolver.resolve(alert.alertName());

		return new EvidenceSignal(
				"alert-evidence:" + alert.alertId(),
				EvidenceLayer.OBSERVABILITY,
				EvidenceSource.PROMETHEUS,
				severityMapper.toEvidenceSeverity(alert.severity()),
				code,
				summary(alert, code),
				observedValue(alert),
				expectedValue(code),
				alert.alertId()
		);
	}

	private String summary(AlertEvent alert, String code) {
		String description = alert.description();

		if (description != null && !description.isBlank()) {
			return description;
		}

		return "Alert evidence detected: " + alert.alertName() + " (" + code + ")";
	}

	private String observedValue(AlertEvent alert) {
		if (alert.annotations() != null) {
			String observed = alert.annotations().get("observedValue");
			if (observed != null && !observed.isBlank()) {
				return observed;
			}

			String value = alert.annotations().get("value");
			if (value != null && !value.isBlank()) {
				return value;
			}
		}

		return "alert=" + alert.alertName()
				+ ", severity=" + alert.severity()
				+ ", service=" + alert.service();
	}

	private String expectedValue(String code) {
		return switch (code) {
			case "LATENCY_SPIKE" -> "latency within SLO";
			case "ERROR_RATE_SPIKE" -> "error rate within SLO";
			case "CONSUMER_LAG_SPIKE" -> "consumer lag within threshold";
			case "CPU_SATURATION" -> "CPU usage below saturation threshold";
			case "MEMORY_PRESSURE" -> "memory usage below pressure threshold";
			case "POD_RESTART_SPIKE" -> "pod restart count within baseline";
			case "DUPLICATE_PAYMENT_RISK" -> "duplicate payment rate at baseline";
			default -> "alert condition resolved";
		};
	}
}
