package com.fintech.sre.agent.recommendation.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertSeverity;
import com.fintech.sre.agent.alert.AlertSource;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.model.response.PolicyDecisionView;

class RecommendationRecordMapperTest {

	@Test
	void shouldMapRecommendationResponseToRecordWithoutRawPayload() {
		RecommendationRecordMapper mapper =
				new RecommendationRecordMapper(new RecommendationRecordIdGenerator());

		AlertEvent alert = new AlertEvent(
				"alert-1",
				AlertSource.PROMETHEUS_ALERTMANAGER,
				"HighP99Latency",
				AlertSeverity.CRITICAL,
				"firing",
				"payment-api",
				"payment",
				"sre-agent",
				"p99 latency high",
				null,
				null,
				Map.of("sensitive", "must-not-be-saved"),
				Map.of()
		);

		ActionCommand command = new ActionCommand(
				"cmd-1",
				ActionType.RATE_LIMIT,
				new ActionTarget("payment", "payment-api", "service", "payment-api", "prod"),
				true,
				new RollbackCommand("rollback"),
				List.of(new VerificationCommand("latency", "normal", "verify latency"))
		);

		IncidentRecommendationResponse response = new IncidentRecommendationResponse(
				"incident-1",
				"READY",
				null,
				List.of(),
				null,
				List.of(new RecommendedAction(
						1,
						"Apply rate limit",
						command,
						"reduce load",
						"temporary limit",
						"rollback",
						List.of("verify latency"),
						true,
						null
				)),
				List.of(),
				List.of(new ForbiddenAction("Restart pod", "unsafe during payment surge")),
				ConfidenceLevel.HIGH,
				true,
				null,
				new PolicyDecisionView("ALLOW", List.of()),
				List.of(),
				"PASS",
				null,
				null
		);

		RecommendationRecord record = mapper.toRecord("audit-1", alert, response);

		assertThat(record.auditId()).isEqualTo("audit-1");
		assertThat(record.service()).isEqualTo("payment-api");
		assertThat(record.metadata()).doesNotContainKey("sensitive");
	}
}
