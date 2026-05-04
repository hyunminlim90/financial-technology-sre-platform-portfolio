package com.fintech.sre.agent.model.common;

import java.util.List;

public record ReferencedKnowledge(
		List<String> scenarios,
		List<String> runbooks,
		List<String> improvements,
		List<String> preventiveDesigns,
		List<String> postmortems,
		List<String> ragDocs
) {
}
