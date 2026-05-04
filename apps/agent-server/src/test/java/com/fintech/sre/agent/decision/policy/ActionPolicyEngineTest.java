package com.fintech.sre.agent.decision.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.evidence.Evidence;
import com.fintech.sre.agent.evidence.EvidenceConfidence;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceLayer;
import com.fintech.sre.agent.evidence.EvidenceQueryStatus;
import com.fintech.sre.agent.evidence.EvidenceSeverity;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.evidence.EvidenceSource;
import com.fintech.sre.agent.evidence.EvidenceStatus;

class ActionPolicyEngineTest {

	private final ActionPolicyEngine policyEngine = new ActionPolicyEngine();

	@Test
	void scaleOutWorkerShouldBeDeniedWhenDatabaseSaturationExists() {
		ActionCommand command = scaleOutWorkerCommand();

		EvidenceContext evidence = new EvidenceContext(
				List.of(new Evidence(
						EvidenceLayer.DATABASE,
						EvidenceSignal.DB_POOL_PENDING_HIGH,
						20,
						10,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.CRITICAL,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"DB pool pending high"
				)),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);

		PolicyEvaluationResult result = policyEngine.evaluate(command, evidence);

		assertThat(result.denied()).isTrue();
		assertThat(result.violations().get(0).policyId()).isEqualTo("scaleout-blocked-db-saturation");
	}

	@Test
	void scaleOutWorkerShouldBeDeniedWhenKafkaRebalanceStormExists() {
		ActionCommand command = scaleOutWorkerCommand();

		EvidenceContext evidence = new EvidenceContext(
				List.of(new Evidence(
						EvidenceLayer.QUEUE,
						EvidenceSignal.KAFKA_REBALANCE_STORM,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.CRITICAL,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Kafka rebalance storm detected"
				)),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);

		PolicyEvaluationResult result = policyEngine.evaluate(command, evidence);

		assertThat(result.denied()).isTrue();
		assertThat(result.violations().get(0).policyId()).isEqualTo("scaleout-blocked-rebalance-storm");
	}

	@Test
	void scaleOutWorkerShouldBeDeniedWhenRetryStormExists() {
		ActionCommand command = scaleOutWorkerCommand();

		EvidenceContext evidence = new EvidenceContext(
				List.of(new Evidence(
						EvidenceLayer.APPLICATION,
						EvidenceSignal.RETRY_STORM,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.CRITICAL,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Retry storm detected"
				)),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);

		PolicyEvaluationResult result = policyEngine.evaluate(command, evidence);

		assertThat(result.denied()).isTrue();
		assertThat(result.violations().get(0).policyId()).isEqualTo("scaleout-blocked-retry-storm");
	}

	@Test
	void actionWithoutRollbackShouldBeDenied() {
		ActionCommand command = new ActionCommand(
				"rate-limit-payment",
				ActionType.RATE_LIMIT,
				new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
				true,
				null,
				List.of(new VerificationCommand("error.rate", "decreasing", "error down"))
		);

		PolicyEvaluationResult result = policyEngine.evaluate(command, kafkaLagEvidence());

		assertThat(result.denied()).isTrue();
		assertThat(result.violations().get(0).policyId()).isEqualTo("rollback-required");
	}

	@Test
	void scaleOutWorkerShouldRequireKafkaLagEvidence() {
		ActionCommand command = scaleOutWorkerCommand();

		EvidenceContext evidence = new EvidenceContext(
				List.of(new Evidence(
						EvidenceLayer.EDGE,
						EvidenceSignal.TRAFFIC_SPIKE,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.MEDIUM,
						EvidenceStatus.PRESENT,
						"Traffic spike"
				)),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);

		PolicyEvaluationResult result = policyEngine.evaluate(command, evidence);

		assertThat(result.decision()).isEqualTo(PolicyEvaluationDecision.INSUFFICIENT_EVIDENCE);
	}

	@Test
	void scaleOutWorkerShouldBeAllowedWhenKafkaLagExistsAndNoBlockingEvidence() {
		PolicyEvaluationResult result = policyEngine.evaluate(scaleOutWorkerCommand(), kafkaLagEvidence());
		assertThat(result.allowed()).isTrue();
		assertThat(result.decision()).isEqualTo(PolicyEvaluationDecision.ALLOW);
	}

	@Test
	void observabilityDegradedShouldRequireApproval() {
		PolicyEvaluationResult result = policyEngine.evaluate(rateLimitCommand(), new EvidenceContext(
				List.of(new Evidence(
						EvidenceLayer.EDGE,
						EvidenceSignal.TRAFFIC_SPIKE,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.MEDIUM,
						EvidenceStatus.PRESENT,
						"Traffic spike"
				)),
				EvidenceQueryStatus.FAILED,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		));
		assertThat(result.decision()).isEqualTo(PolicyEvaluationDecision.REQUIRE_APPROVAL);
		assertThat(result.allowed()).isTrue();
	}

	private EvidenceContext kafkaLagEvidence() {
		return new EvidenceContext(
				List.of(new Evidence(
						EvidenceLayer.QUEUE,
						EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH,
						5000,
						1000,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Kafka consumer lag high"
				)),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);
	}

	private ActionCommand scaleOutWorkerCommand() {
		return new ActionCommand(
				"scale-out-payment",
				ActionType.SCALE_OUT,
				new ActionTarget("payment", "payment-worker", "queue", "payment-worker", "prod"),
				true,
				new RollbackCommand("Scale back replicas"),
				List.of(new VerificationCommand("lag", "decreasing", "Lag decreases"))
		);
	}

	private ActionCommand rateLimitCommand() {
		return new ActionCommand(
				"rate-limit-payment",
				ActionType.RATE_LIMIT,
				new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
				true,
				new RollbackCommand("Remove rate limit"),
				List.of(new VerificationCommand("error.rate", "decreasing", "error down"))
		);
	}
}
