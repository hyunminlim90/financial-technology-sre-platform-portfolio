package com.fintech.sre.agent.policy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.evidence.EvidenceContext;

import reactor.core.publisher.Mono;

@Component
public class PaymentSafetyPolicyRule implements PolicyRule {

	@Override
	public Mono<PolicyEvaluationResult> evaluate(ActionCommand command, EvidenceContext evidence) {
		List<PolicyViolation> violations = new ArrayList<>();

		if (command == null || command.target() == null) {
			return Mono.just(PolicyEvaluationResult.allow());
		}

		boolean paymentDomain = "payment".equalsIgnoreCase(command.target().domain())
				|| "payment-service".equalsIgnoreCase(command.target().service())
				|| (command.target().service() != null && command.target().service().contains("payment"));

		if (!paymentDomain) {
			return Mono.just(PolicyEvaluationResult.allow());
		}

		if (command.type() == ActionType.SCALE_DOWN) {
			violations.add(new PolicyViolation(
					"PAYMENT_SCALE_DOWN_BLOCKED",
					PolicySeverity.BLOCKING,
					"결제 도메인에서는 Scale Down 추천을 차단합니다."
			));
		}

		if (command.type() == ActionType.RESTART_POD) {
			violations.add(new PolicyViolation(
					"PAYMENT_RESTART_POD_BLOCKED",
					PolicySeverity.BLOCKING,
					"결제 도메인에서는 Pod Restart 추천을 차단합니다."
			));
		}

		if (command.type() == ActionType.FAILOVER) {
			violations.add(new PolicyViolation(
					"PAYMENT_FAILOVER_REVIEW_REQUIRED",
					PolicySeverity.WARNING,
					"결제 도메인 Failover는 멱등성/정합성 확인 후 Human이 검토해야 합니다."
			));
		}

		if (!hasPaymentSafetyVerification(command)) {
			violations.add(new PolicyViolation(
					"PAYMENT_SAFETY_VERIFICATION_REQUIRED",
					PolicySeverity.BLOCKING,
					"결제 도메인 Action에는 idempotency/duplicate payment/consistency 검증이 필요합니다."
			));
		}

		boolean hasBlocking = violations.stream()
				.anyMatch(violation -> violation.severity() == PolicySeverity.BLOCKING);

		return Mono.just(hasBlocking
				? PolicyEvaluationResult.deny(violations)
				: new PolicyEvaluationResult(true, violations));
	}

	private boolean hasPaymentSafetyVerification(ActionCommand command) {
		if (command.verifications() == null) {
			return false;
		}

		return command.verifications().stream()
				.anyMatch(verification -> contains(verification.metric(), "idempotency")
						|| contains(verification.metric(), "duplicate")
						|| contains(verification.metric(), "consistency")
						|| contains(verification.description(), "멱등성")
						|| contains(verification.description(), "중복 결제")
						|| contains(verification.description(), "정합성"));
	}

	private boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase().contains(keyword.toLowerCase());
	}
}
