package com.fintech.sre.agent.runtime.reliability;

import java.time.Instant;
import java.util.Objects;

public record EvidenceQuery(
		EvidenceSourceType sourceType,
		String subjectId,
		Instant from,
		Instant to,
		boolean paymentRelated
) {
	public EvidenceQuery {
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		Objects.requireNonNull(subjectId, "subjectId must not be null");
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");

		if (subjectId.isBlank()) {
			throw new IllegalArgumentException("subjectId must not be blank");
		}
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
	}

	public boolean vendorNeutral() {
		return true;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
