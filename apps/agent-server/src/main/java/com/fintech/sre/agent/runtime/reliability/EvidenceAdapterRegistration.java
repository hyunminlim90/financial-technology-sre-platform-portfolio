package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceAdapterRegistration(
		EvidenceAdapterDescriptor descriptor,
		EvidenceAdapterPort adapter
) {
	public EvidenceAdapterRegistration {
		Objects.requireNonNull(descriptor, "descriptor must not be null");
	}

	public boolean runtimeDiscoveryMetadata() {
		return true;
	}

	public boolean activation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean healthCheckResult() {
		return false;
	}
}
