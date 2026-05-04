package com.fintech.sre.agent.runbook;

import java.util.List;

public record RunbookVerification(
		boolean required,
		List<String> checks
) {
}
