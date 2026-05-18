package com.fintech.sre.agent.alert.evidence;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.alert.AlertSeverity;
import com.fintech.sre.agent.evidence.EvidenceSeverity;

@Component
public class AlertEvidenceSeverityMapper {

	public EvidenceSeverity toEvidenceSeverity(AlertSeverity severity) {
		if (severity == null) {
			return EvidenceSeverity.WARNING;
		}

		return switch (severity) {
			case CRITICAL -> EvidenceSeverity.CRITICAL;
			case WARNING -> EvidenceSeverity.WARNING;
			case INFO, UNKNOWN -> EvidenceSeverity.INFO;
		};
	}
}
