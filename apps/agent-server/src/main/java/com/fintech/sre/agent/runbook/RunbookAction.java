package com.fintech.sre.agent.runbook;

import java.util.List;

public record RunbookAction(
		String type,
		String targetLayer,
		String targetService,
		String riskLevel,
		String blastRadius,
		String description,
		List<String> preconditions,
		List<String> forbiddenIf,
		RunbookApproval approval,
		RunbookRollback rollback,
		RunbookVerification verification,
		RunbookPaymentSafety paymentSafety
) {
}
