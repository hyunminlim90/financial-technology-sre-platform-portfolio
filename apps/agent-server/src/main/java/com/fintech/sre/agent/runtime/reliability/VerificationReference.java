package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record VerificationReference(
		String verificationId,
		String knowledgeSourceId,
		boolean known,
		boolean deprecated,
		boolean paymentConsistencyVerification
) {
	public VerificationReference {
		Objects.requireNonNull(
				verificationId,
				"verificationId must not be null"
		);
		Objects.requireNonNull(
				knowledgeSourceId,
				"knowledgeSourceId must not be null"
		);
	}
}
