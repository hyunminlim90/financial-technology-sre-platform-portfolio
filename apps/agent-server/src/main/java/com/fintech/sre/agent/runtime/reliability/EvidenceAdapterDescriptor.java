package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceAdapterDescriptor(
		String adapterId,
		String adapterName,
		EvidenceSourceType sourceType,
		EvidenceAdapterAvailability availability,
		boolean vendorNeutral,
		boolean supportsPaymentEvidence
) {
	public EvidenceAdapterDescriptor {
		Objects.requireNonNull(adapterId, "adapterId must not be null");
		Objects.requireNonNull(adapterName, "adapterName must not be null");
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		Objects.requireNonNull(availability, "availability must not be null");

		if (adapterId.isBlank()) {
			throw new IllegalArgumentException("adapterId must not be blank");
		}
		if (adapterName.isBlank()) {
			throw new IllegalArgumentException("adapterName must not be blank");
		}
	}

	public boolean exposesRawCredentialOrConfiguration() {
		return false;
	}
}
