package com.fintech.sre.agent.postmortem.draft;

import java.util.Map;

public record PostmortemDraftRequest(
		String requestedBy,
		String reason,
		Map<String, String> metadata
) {
}
