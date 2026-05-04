package com.fintech.sre.agent.policy;

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

import reactor.test.StepVerifier;

class PolicyEngineTest {

	private final PolicyEngine policyEngine = new PolicyEngine(List.of(
			new HumanApprovalRequiredPolicyRule(),
			new RollbackRequiredPolicyRule(),
			new VerificationRequiredPolicyRule(),
			new PaymentSafetyPolicyRule()
	));

	@Test
	void shouldBlockActionWithoutHumanApproval() {
		StepVerifier.create(policyEngine.evaluate(
				new ActionCommand(
						"rate-limit-payment",
						ActionType.RATE_LIMIT,
						new ActionTarget("payment", "payment-api", "policy", "rate-limit", "prod"),
						false,
						new RollbackCommand("Remove rate limit"),
						List.of(new VerificationCommand("payment.consistency", "stable", "결제 정합성 확인"))
				),
				evidence()
		))
				.expectNextMatches(result ->
						!result.allowed()
								&& result.violations().stream()
								.anyMatch(violation -> "POLICY_HUMAN_APPROVAL_REQUIRED".equals(violation.code()))
				)
				.verifyComplete();
	}

	@Test
	void shouldBlockPaymentScaleDown() {
		StepVerifier.create(policyEngine.evaluate(
				new ActionCommand(
						"scale-down-payment",
						ActionType.SCALE_DOWN,
						new ActionTarget("payment", "payment-service", "k8s-deployment", "payment-service", "prod"),
						true,
						new RollbackCommand("Restore previous replicas"),
						List.of(new VerificationCommand("payment.consistency", "stable", "결제 정합성 확인"))
				),
				evidence()
		))
				.expectNextMatches(result ->
						!result.allowed()
								&& result.violations().stream()
								.anyMatch(violation -> "PAYMENT_SCALE_DOWN_BLOCKED".equals(violation.code()))
				)
				.verifyComplete();
	}

	@Test
	void shouldBlockPaymentActionWithoutSafetyVerification() {
		StepVerifier.create(policyEngine.evaluate(
				new ActionCommand(
						"rate-limit-payment",
						ActionType.RATE_LIMIT,
						new ActionTarget("payment", "payment-service", "policy", "rate-limit", "prod"),
						true,
						new RollbackCommand("Remove rate limit"),
						List.of(new VerificationCommand("error.rate", "decreasing", "error 감소 확인"))
				),
				evidence()
		))
				.expectNextMatches(result ->
						!result.allowed()
								&& result.violations().stream()
								.anyMatch(violation -> "PAYMENT_SAFETY_VERIFICATION_REQUIRED".equals(violation.code()))
				)
				.verifyComplete();
	}

	@Test
	void shouldAllowPaymentActionWithSafetyVerification() {
		StepVerifier.create(policyEngine.evaluate(
				new ActionCommand(
						"rate-limit-payment",
						ActionType.RATE_LIMIT,
						new ActionTarget("payment", "payment-service", "policy", "rate-limit", "prod"),
						true,
						new RollbackCommand("Remove rate limit"),
						List.of(
								new VerificationCommand("error.rate", "decreasing", "error 감소 확인"),
								new VerificationCommand("payment.consistency", "stable", "결제 정합성 확인")
						)
				),
				evidence()
		))
				.expectNextMatches(result -> result.allowed() && result.violations().isEmpty())
				.verifyComplete();
	}

	private EvidenceContext evidence() {
		return new EvidenceContext(
				"INC-POLICY-1",
				"payment-service",
				"prod",
				List.of(new Evidence(
						EvidenceLayer.APPLICATION,
						EvidenceSignal.ERROR_RATE_HIGH,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"error spike"
				)),
				java.util.Map.of("domain", "payment"),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);
	}
}
