package com.fintech.sre.agent.runbook;

public record RunbookRollback(
		boolean required,
		String plan
) {
}
