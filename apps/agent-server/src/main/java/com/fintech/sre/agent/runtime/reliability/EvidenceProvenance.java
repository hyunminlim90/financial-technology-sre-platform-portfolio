package com.fintech.sre.agent.runtime.reliability;

import java.time.Instant;

public record EvidenceProvenance(
		EvidenceSourceType sourceType,
		String adapterId,
		Instant collectedAt,
		boolean sanitized,
		boolean rawPayloadPresent,
		boolean sensitiveDataPresent
) {
	public static EvidenceProvenance missingProvenance() {
		return new EvidenceProvenance(
				null,
				null,
				null,
				false,
				false,
				false
		);
	}

	public boolean provenanceMissing() {
		return sourceType == null && adapterId == null && collectedAt == null;
	}

	public boolean unknown() {
		return !provenanceMissing()
				&& (adapterId == null || adapterId.isBlank() || collectedAt == null);
	}
}
