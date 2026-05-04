package com.fintech.sre.agent.model.request;

import jakarta.validation.constraints.NotBlank;

public record PostmortemGenerateByIncidentRequest(
		@NotBlank String incidentId,
		String operatorSummary
) {
}
