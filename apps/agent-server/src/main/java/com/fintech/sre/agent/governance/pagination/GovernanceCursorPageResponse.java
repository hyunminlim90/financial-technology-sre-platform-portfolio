package com.fintech.sre.agent.governance.pagination;

import java.util.List;

public record GovernanceCursorPageResponse<T>(
		List<T> items,
		GovernanceCursorMetadata page
) {
}
