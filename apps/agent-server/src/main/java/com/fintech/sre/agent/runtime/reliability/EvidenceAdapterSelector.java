package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class EvidenceAdapterSelector {

	public EvidenceAdapterSelection select(
			EvidenceAdapterRegistry registry,
			EvidenceSourceType sourceType,
			EvidenceAdapterSelectionScope scope
	) {
		Objects.requireNonNull(registry, "registry must not be null");
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		Objects.requireNonNull(scope, "scope must not be null");

		List<EvidenceAdapterRegistration> registrations =
				registry.findBySourceType(sourceType);
		if (registrations.isEmpty()) {
			return rejected(
					sourceType,
					scope,
					policies(scope),
					EvidenceAdapterSelectionRejectionReason.NO_REGISTERED_ADAPTER
			);
		}

		List<EvidenceAdapterRegistration> eligible = eligible(registrations, scope);
		if (scope == EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE
				&& registrations.stream().noneMatch(registration -> registration
						.descriptor()
						.supportsPaymentEvidence())) {
			return rejected(
					sourceType,
					scope,
					policies(scope),
					EvidenceAdapterSelectionRejectionReason
							.PAYMENT_EVIDENCE_NOT_SUPPORTED
			);
		}

		if (eligible.isEmpty()) {
			return rejected(
					sourceType,
					scope,
					policies(scope),
					scope == EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE
							? EvidenceAdapterSelectionRejectionReason.NO_AVAILABLE_ADAPTER
							: EvidenceAdapterSelectionRejectionReason.NO_AVAILABLE_ADAPTER
			);
		}

		return new EvidenceAdapterSelection(
				sourceType,
				scope,
				policies(scope),
				eligible,
				null
		);
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

	private EvidenceAdapterSelection rejected(
			EvidenceSourceType sourceType,
			EvidenceAdapterSelectionScope scope,
			List<EvidenceAdapterSelectionPolicy> policies,
			EvidenceAdapterSelectionRejectionReason rejectionReason
	) {
		return new EvidenceAdapterSelection(
				sourceType,
				scope,
				policies,
				List.of(),
				rejectionReason
		);
	}

	private List<EvidenceAdapterRegistration> eligible(
			List<EvidenceAdapterRegistration> registrations,
			EvidenceAdapterSelectionScope scope
	) {
		return registrations.stream()
				.filter(registration -> supportsScope(registration.descriptor(), scope))
				.filter(registration -> allowedAvailability(
						registration.descriptor().availability(),
						scope
				))
				.toList();
	}

	private boolean supportsScope(
			EvidenceAdapterDescriptor descriptor,
			EvidenceAdapterSelectionScope scope
	) {
		if (scope == EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE) {
			return descriptor.supportsPaymentEvidence()
					&& descriptor.availability()
					== EvidenceAdapterAvailability.AVAILABLE;
		}
		return true;
	}

	private boolean allowedAvailability(
			EvidenceAdapterAvailability availability,
			EvidenceAdapterSelectionScope scope
	) {
		return switch (scope) {
			case STANDARD_EVIDENCE -> availability == EvidenceAdapterAvailability.AVAILABLE;
			case PAYMENT_CONSISTENCY_EVIDENCE ->
					availability == EvidenceAdapterAvailability.AVAILABLE;
			case RESTRICTED_FALLBACK ->
					availability == EvidenceAdapterAvailability.DEPRECATED;
			case UNCERTAIN_FALLBACK ->
					availability == EvidenceAdapterAvailability.UNKNOWN;
		};
	}

	private List<EvidenceAdapterSelectionPolicy> policies(
			EvidenceAdapterSelectionScope scope
	) {
		return switch (scope) {
			case STANDARD_EVIDENCE ->
					List.of(EvidenceAdapterSelectionPolicy.PREFER_AVAILABLE);
			case PAYMENT_CONSISTENCY_EVIDENCE ->
					List.of(
							EvidenceAdapterSelectionPolicy.PREFER_AVAILABLE,
							EvidenceAdapterSelectionPolicy
									.REQUIRE_PAYMENT_EVIDENCE_SUPPORT
					);
			case RESTRICTED_FALLBACK ->
					List.of(
							EvidenceAdapterSelectionPolicy.PREFER_AVAILABLE,
							EvidenceAdapterSelectionPolicy
									.ALLOW_DEPRECATED_RESTRICTED
					);
			case UNCERTAIN_FALLBACK ->
					List.of(
							EvidenceAdapterSelectionPolicy.PREFER_AVAILABLE,
							EvidenceAdapterSelectionPolicy.ALLOW_UNKNOWN_UNCERTAIN
					);
		};
	}
}
