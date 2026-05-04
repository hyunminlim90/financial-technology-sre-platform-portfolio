package com.fintech.sre.agent.guardrail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.model.common.ActionSource;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.Evidence;
import com.fintech.sre.agent.model.common.IncidentSummary;
import com.fintech.sre.agent.model.common.ImpactScope;
import com.fintech.sre.agent.model.common.MetricEvidence;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.common.ReferencedKnowledge;
import com.fintech.sre.agent.model.common.Severity;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

class ActionCommandGuardrailTest {

	private final ActionCommandGuardrail guardrail = new ActionCommandGuardrail();

	@Test
	void failsWhenActionCommandIsMissing() {
		IncidentRecommendationResponse response = responseWithAction(new RecommendedAction(
				1,
				"Apply controlled rate limiting",
				null,
				"Reduce traffic",
				"Low operational risk",
				"Remove rate limit",
				List.of("latency down"),
				true,
				ActionSource.RUNBOOK
		));

		assertThatThrownBy(() -> guardrail.validate(response).block())
				.isInstanceOf(GuardrailViolationException.class)
				.hasMessageContaining("ActionCommand");
	}

	@Test
	void failsWhenActionDoesNotRequireHumanApproval() {
		IncidentRecommendationResponse response = responseWithAction(new RecommendedAction(
				1,
				"Apply rate limit",
				new ActionCommand(
						"rate-limit-payment",
						ActionType.RATE_LIMIT,
						new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
						false,
						new RollbackCommand("Remove rate limit"),
						List.of(new VerificationCommand("error.rate", "decreasing", "error down"))
				),
				"Reduce traffic",
				"Moderate operational risk",
				"Remove rate limit",
				List.of("error down"),
				true,
				ActionSource.RUNBOOK
		));

		assertThatThrownBy(() -> guardrail.validate(response).block())
				.isInstanceOf(GuardrailViolationException.class)
				.hasMessageContaining("Human approval");
	}

	@Test
	void failsWhenNoEvidenceSupportsAction() {
		IncidentRecommendationResponse response = new IncidentRecommendationResponse(
				"INC-DSL-2",
				"RECOMMENDATION_CREATED",
				new IncidentSummary("redis-timeout", "redis", "payment-api", "prod", Severity.SEV_2, ImpactScope.PARTIAL),
				List.of(),
				new Evidence(List.of(), List.of(), List.of()),
				List.of(new RecommendedAction(
						1,
						"Apply rate limit",
						new ActionCommand(
								"rate-limit-payment",
								ActionType.RATE_LIMIT,
								new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
								true,
								new RollbackCommand("Remove rate limit"),
								List.of(new VerificationCommand("error.rate", "decreasing", "error down"))
						),
						"Reduce traffic",
						"Moderate operational risk",
						"Remove rate limit",
						List.of("error down"),
						true,
						ActionSource.RUNBOOK
				)),
				List.of(),
				List.of(),
				ConfidenceLevel.MEDIUM,
				true,
				new ReferencedKnowledge(List.of(), List.of(), List.of(), List.of(), List.of(), List.of())
		);

		assertThatThrownBy(() -> guardrail.validate(response).block())
				.isInstanceOf(GuardrailViolationException.class)
				.hasMessageContaining("Observed evidence");
	}

	private IncidentRecommendationResponse responseWithAction(RecommendedAction action) {
		return new IncidentRecommendationResponse(
				"INC-DSL-1",
				"RECOMMENDATION_CREATED",
				new IncidentSummary("redis-timeout", "redis", "payment-api", "prod", Severity.SEV_2, ImpactScope.PARTIAL),
				List.of(),
				new Evidence(
						List.of(
								new MetricEvidence("error_rate", 0.12, 0.01, "abnormal", "sum(rate(errors))"),
								new MetricEvidence("p95_latency_ms", 920.0, 300.0, "abnormal", "histogram_quantile(...)")
						),
						List.of(),
						List.of()
				),
				List.of(action),
				List.of(),
				List.of(),
				ConfidenceLevel.MEDIUM,
				true,
				new ReferencedKnowledge(List.of(), List.of(), List.of(), List.of(), List.of(), List.of())
		);
	}
}
