package com.fintech.sre.agent.action;

import java.util.List;

public record ActionCommand(
		String id,
		ActionType type,
		ActionTarget target,
		boolean requiresHumanApproval,
		RollbackCommand rollback,
		List<VerificationCommand> verifications
) {
}
