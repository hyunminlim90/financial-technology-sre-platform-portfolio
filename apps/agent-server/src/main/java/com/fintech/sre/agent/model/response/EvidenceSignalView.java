package com.fintech.sre.agent.model.response;

import com.fintech.sre.agent.evidence.EvidenceSignal;

public record EvidenceSignalView(
		String id,
		String layer,
		String source,
		String severity,
		String code,
		String summary,
		String observedValue,
		String expectedValue,
		String reference
) {
	public static EvidenceSignalView from(EvidenceSignal signal) {
		return new EvidenceSignalView(
				signal.id(),
				signal.layer().name(),
				signal.source().name(),
				signal.severity().name(),
				signal.code(),
				signal.summary(),
				signal.observedValue(),
				signal.expectedValue(),
				signal.reference()
		);
	}
}
