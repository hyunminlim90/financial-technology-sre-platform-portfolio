package com.fintech.sre.agent.application;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.exception.InsufficientEvidenceException;
import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;

import reactor.core.publisher.Mono;

@Component
public class IncidentRequestValidator {

	public Mono<Void> validate(IncidentAnalyzeRequest request) {
		if (request == null) {
			return Mono.error(new IllegalArgumentException("request must not be null"));
		}

		boolean hasMetrics = request.metricsSnapshot() != null;
		boolean hasLogs = request.logsSample() != null && !request.logsSample().isEmpty();
		boolean hasTraces = request.traceIds() != null && !request.traceIds().isEmpty();

		if (!hasMetrics && !hasLogs && !hasTraces) {
			return Mono.error(new InsufficientEvidenceException(
					"metrics/logs/traces 중 최소 하나 이상의 근거가 필요합니다."
			));
		}

		return Mono.empty();
	}
}
