package com.fintech.sre.agent.runbook;

import java.util.List;

public record RunbookCondition(
		List<String> all,
		List<String> any,
		List<String> none
) {
}
