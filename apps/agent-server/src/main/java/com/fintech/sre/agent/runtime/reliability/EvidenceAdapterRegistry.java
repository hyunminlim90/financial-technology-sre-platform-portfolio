package com.fintech.sre.agent.runtime.reliability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EvidenceAdapterRegistry {

	private final Map<String, EvidenceAdapterRegistration> registrationsById;

	private EvidenceAdapterRegistry(
			Map<String, EvidenceAdapterRegistration> registrationsById
	) {
		this.registrationsById = Map.copyOf(registrationsById);
	}

	public static EvidenceAdapterRegistry empty() {
		return new EvidenceAdapterRegistry(Map.of());
	}

	public static EvidenceAdapterRegistry of(
			List<EvidenceAdapterRegistration> registrations
	) {
		Objects.requireNonNull(registrations, "registrations must not be null");

		EvidenceAdapterRegistry registry = empty();
		for (EvidenceAdapterRegistration registration : registrations) {
			registry = registry.register(registration);
		}
		return registry;
	}

	public EvidenceAdapterRegistry register(
			EvidenceAdapterRegistration registration
	) {
		Objects.requireNonNull(registration, "registration must not be null");
		validate(registration.descriptor());

		if (registrationsById.containsKey(registration.descriptor().adapterId())) {
			throw new IllegalArgumentException(
					EvidenceAdapterRejectionReason.DUPLICATE_ADAPTER_ID.name()
			);
		}

		Map<String, EvidenceAdapterRegistration> updated = new LinkedHashMap<>(
				registrationsById
		);
		updated.put(registration.descriptor().adapterId(), registration);
		return new EvidenceAdapterRegistry(updated);
	}

	public List<EvidenceAdapterRegistration> findBySourceType(
			EvidenceSourceType sourceType
	) {
		Objects.requireNonNull(sourceType, "sourceType must not be null");

		return registrationsById.values().stream()
				.filter(registration -> registration.descriptor().sourceType()
						== sourceType)
				.toList();
	}

	public List<EvidenceAdapterDescriptor> descriptors() {
		return registrationsById.values().stream()
				.map(EvidenceAdapterRegistration::descriptor)
				.toList();
	}

	public boolean discoveryOnly() {
		return true;
	}

	public boolean executesEvidenceQuery() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean systemFailure() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private void validate(EvidenceAdapterDescriptor descriptor) {
		if (!descriptor.vendorNeutral()) {
			throw new IllegalArgumentException(
					EvidenceAdapterRejectionReason.INVALID_DESCRIPTOR.name()
			);
		}
		if (descriptor.sourceType() == null) {
			throw new IllegalArgumentException(
					EvidenceAdapterRejectionReason.UNSUPPORTED_SOURCE_TYPE.name()
			);
		}
	}
}
