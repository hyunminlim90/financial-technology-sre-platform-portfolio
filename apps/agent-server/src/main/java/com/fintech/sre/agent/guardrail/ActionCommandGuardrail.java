package com.fintech.sre.agent.guardrail;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(2)
public class ActionCommandGuardrail implements Guardrail {

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		if (response.evidence() == null
				|| ((response.evidence().metrics() == null || response.evidence().metrics().isEmpty())
				&& (response.evidence().logs() == null || response.evidence().logs().isEmpty())
				&& (response.evidence().traces() == null || response.evidence().traces().isEmpty()))) {
			return Mono.error(new GuardrailViolationException(
					"MISSING_EVIDENCE_JUSTIFICATION",
					"Observed evidence 없이 Action을 정당화할 수 없습니다"
			));
		}

		for (RecommendedAction action : response.recommendedActions()) {
			ActionCommand command = action.command();
			if (command == null) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_COMMAND",
						"ActionCommand가 없습니다"
				));
			}
			if (command.type() == null) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_ACTION_TYPE",
						"ActionCommand.type must not be null."
				));
			}
			if (!command.requiresHumanApproval()) {
				return Mono.error(new GuardrailViolationException(
						"HUMAN_APPROVAL_REQUIRED",
						"Human approval 없는 action 금지"
				));
			}
			if (command.isHighRiskOrAbove() && !command.requiresHumanApproval()) {
				return Mono.error(new GuardrailViolationException(
						"HIGH_RISK_HUMAN_APPROVAL_REQUIRED",
						"HIGH 이상 위험 Action은 Human approval 없이는 추천할 수 없습니다"
				));
			}
			if (command.rollback() == null) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_ROLLBACK",
						"Rollback 없는 action 금지"
				));
			}
			if (command.verifications() == null || command.verifications().isEmpty()) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_VERIFICATION",
						"Verification 없는 action 금지"
				));
			}
			if (command.isPaymentDomain()
					&& command.paymentSafety() != null
					&& command.paymentSafety().unsafe()) {
				return Mono.error(new GuardrailViolationException(
						"UNSAFE_PAYMENT_ACTION",
						"결제 도메인 Action은 payment safety 조건을 만족해야 합니다"
				));
			}
			if (!hasSupportingEvidence(response, command)) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_SUPPORTING_EVIDENCE",
						"ActionCommand를 뒷받침하는 EvidenceSignal이 없습니다"
				));
			}
		}

		return Mono.just(response);
	}

	private boolean hasSupportingEvidence(IncidentRecommendationResponse response, ActionCommand command) {
		return switch (command.type()) {
			case RATE_LIMIT -> hasAbnormalMetric(response, "error_rate")
					|| hasAbnormalMetric(response, "error.rate")
					|| hasAbnormalMetric(response, "p95_latency_ms")
					|| hasAbnormalMetric(response, "db_connection_pending")
					|| hasLogContaining(response, "timeout")
					|| hasLogContaining(response, "error");
			case SCALE_OUT -> hasAbnormalMetric(response, "p95_latency_ms")
					|| hasAbnormalMetric(response, "latency.p95")
					|| hasAbnormalMetric(response, "kafka_consumer_lag");
			default -> response.evidence() != null
					&& ((response.evidence().metrics() != null && !response.evidence().metrics().isEmpty())
					|| (response.evidence().logs() != null && !response.evidence().logs().isEmpty())
					|| (response.evidence().traces() != null && !response.evidence().traces().isEmpty()));
		};
	}

	private boolean hasAbnormalMetric(IncidentRecommendationResponse response, String metricName) {
		return response.evidence() != null
				&& response.evidence().metrics() != null
				&& response.evidence().metrics().stream()
				.anyMatch(metric -> metricName.equalsIgnoreCase(metric.name())
						&& "abnormal".equalsIgnoreCase(metric.status()));
	}

	private boolean hasLogContaining(IncidentRecommendationResponse response, String keyword) {
		return response.evidence() != null
				&& response.evidence().logs() != null
				&& response.evidence().logs().stream()
				.anyMatch(log -> log != null && log.toLowerCase().contains(keyword.toLowerCase()));
	}
}
