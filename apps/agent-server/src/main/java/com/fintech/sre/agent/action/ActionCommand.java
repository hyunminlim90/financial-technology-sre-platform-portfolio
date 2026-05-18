package com.fintech.sre.agent.action;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ActionCommand(
		String id,
		ActionType type,
		ActionTarget target,
		RiskLevel riskLevel,
		BlastRadius blastRadius,
		ApprovalPolicy approvalPolicy,
		RollbackPolicy rollbackPolicy,
		VerificationPolicy verificationPolicy,
		PaymentSafety paymentSafety,
		List<String> preconditions,
		List<String> forbiddenIf,
		RollbackCommand rollback,
		List<VerificationCommand> verifications,
		String humanReadableDescription
) {
	public ActionCommand {
		preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
		forbiddenIf = forbiddenIf == null ? List.of() : List.copyOf(forbiddenIf);
		verifications = verifications == null ? List.of() : List.copyOf(verifications);
	}

	public ActionCommand(
			String id,
			ActionType type,
			ActionTarget target,
			boolean requiresHumanApproval,
			RollbackCommand rollback,
			List<VerificationCommand> verifications
	) {
		this(
				id,
				type,
				target,
				RiskLevel.HIGH,
				BlastRadius.SERVICE,
				new ApprovalPolicy(requiresHumanApproval),
				RollbackPolicy.requiredPolicy(),
				VerificationPolicy.required(extractChecks(verifications)),
				defaultPaymentSafety(target),
				List.of(),
				List.of(),
				rollback,
				verifications,
				null
		);
	}

	@JsonIgnore
	public boolean requiresHumanApproval() {
		return approvalPolicy != null && approvalPolicy.required();
	}

	@JsonIgnore
	public boolean hasRollback() {
		return rollback != null
				&& rollback.description() != null
				&& !rollback.description().isBlank();
	}

	@JsonIgnore
	public boolean hasVerification() {
		return verifications != null && !verifications.isEmpty();
	}

	@JsonIgnore
	public boolean isHighRiskOrAbove() {
		return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
	}

	@JsonIgnore
	public boolean isPaymentDomain() {
		return target != null
				&& ("payment".equalsIgnoreCase(target.domain())
				|| contains(target.service(), "payment"));
	}

	private static List<String> extractChecks(List<VerificationCommand> verifications) {
		if (verifications == null) {
			return List.of();
		}
		return verifications.stream()
				.map(VerificationCommand::description)
				.toList();
	}

	private static PaymentSafety defaultPaymentSafety(ActionTarget target) {
		if (target != null && ("payment".equalsIgnoreCase(target.domain()) || contains(target.service(), "payment"))) {
			return PaymentSafety.requiredSafe();
		}
		return new PaymentSafety(true, true, DuplicateExecutionRisk.LOW);
	}

	private static boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase().contains(keyword.toLowerCase());
	}
}
