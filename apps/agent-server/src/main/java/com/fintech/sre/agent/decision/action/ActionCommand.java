package com.fintech.sre.agent.decision.action;

import java.util.List;

public record ActionCommand(
		ActionType type,
		TargetLayer targetLayer,
		String targetService,
		RiskLevel riskLevel,
		BlastRadius blastRadius,
		List<String> preconditions,
		List<String> forbiddenIf,
		ApprovalPolicy approvalPolicy,
		RollbackPolicy rollbackPolicy,
		VerificationPolicy verificationPolicy,
		PaymentSafety paymentSafety,
		String humanReadableDescription
) {
}
