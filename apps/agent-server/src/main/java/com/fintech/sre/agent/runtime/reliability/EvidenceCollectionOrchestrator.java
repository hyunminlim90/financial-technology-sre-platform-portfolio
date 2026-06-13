package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EvidenceCollectionOrchestrator {

	public EvidenceCollectionResult collect(
			EvidenceCollectionRequest request
	) {
		Objects.requireNonNull(request, "request must not be null");

		if (request.adapters().isEmpty()) {
			return rejected(
					EvidenceCollectionRejectionReason.NO_ADAPTERS_CONFIGURED
			);
		}
		if (request.adapters().size() != request.queries().size()) {
			return rejected(
					EvidenceCollectionRejectionReason.ADAPTER_QUERY_SIZE_MISMATCH
			);
		}

		List<EvidenceQueryResult> adapterResults = new ArrayList<>();
		for (int i = 0; i < request.adapters().size(); i++) {
			adapterResults.add(
					request.adapters().get(i).collect(request.queries().get(i))
			);
		}

		List<EvidenceSignal> normalizedSignals = adapterResults.stream()
				.flatMap(result -> result.signals().stream())
				.toList();

		boolean paymentSafetyUncertain = adapterResults.stream()
				.noneMatch(result -> result.sourceType()
						== EvidenceSourceType.PAYMENT_CONSISTENCY
						&& result.paymentConsistencyMetadataPresent());

		boolean contradictionMarkerPresent = contradictionDetected(normalizedSignals);
		EvidenceCollectionStatus status = statusOf(adapterResults);
		OperationalUncertainty uncertainty = uncertaintyOf(
				status,
				paymentSafetyUncertain,
				contradictionMarkerPresent
		);

		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				adapterResults,
				normalizedSignals,
				status,
				paymentSafetyUncertain,
				contradictionMarkerPresent,
				uncertainty,
				null
		);
	}

	private EvidenceCollectionResult rejected(
			EvidenceCollectionRejectionReason rejectionReason
	) {
		return new EvidenceCollectionResult(
				List.of(),
				List.of(),
				List.of(),
				EvidenceCollectionStatus.FAILED,
				true,
				false,
				OperationalUncertainty.HIGH,
				rejectionReason
		);
	}

	private boolean contradictionDetected(List<EvidenceSignal> normalizedSignals) {
		Map<String, List<EvidenceSignal>> grouped = normalizedSignals.stream()
				.collect(Collectors.groupingBy(signal -> signal.type().name()
						+ ":" + signal.signalId()));
		return grouped.values().stream().anyMatch(signals -> signals.stream()
				.map(EvidenceSignal::summary)
				.distinct()
				.count() > 1);
	}

	private EvidenceCollectionStatus statusOf(List<EvidenceQueryResult> results) {
		boolean anyCollected = results.stream().anyMatch(result -> result.status()
				== EvidenceCollectionStatus.COLLECTED);
		boolean anyPartial = results.stream().anyMatch(result -> result.status()
				== EvidenceCollectionStatus.PARTIAL);
		boolean anyAbsent = results.stream().anyMatch(result -> result.status()
				== EvidenceCollectionStatus.ABSENT);
		boolean anyUnknown = results.stream().anyMatch(result -> result.status()
				== EvidenceCollectionStatus.UNKNOWN);
		boolean anyFailed = results.stream().anyMatch(result -> result.status()
				== EvidenceCollectionStatus.FAILED);
		boolean allFailed = results.stream().allMatch(result -> result.status()
				== EvidenceCollectionStatus.FAILED);

		if (allFailed) {
			return EvidenceCollectionStatus.FAILED;
		}
		if (!anyCollected && !anyPartial && !anyAbsent && anyUnknown) {
			return EvidenceCollectionStatus.UNKNOWN;
		}
		if (anyFailed || anyUnknown || anyPartial) {
			return EvidenceCollectionStatus.PARTIAL;
		}
		if (anyCollected) {
			return EvidenceCollectionStatus.COLLECTED;
		}
		return anyAbsent ? EvidenceCollectionStatus.ABSENT
				: EvidenceCollectionStatus.UNKNOWN;
	}

	private OperationalUncertainty uncertaintyOf(
			EvidenceCollectionStatus status,
			boolean paymentSafetyUncertain,
			boolean contradictionMarkerPresent
	) {
		if (contradictionMarkerPresent) {
			return OperationalUncertainty.HIGH;
		}
		if (paymentSafetyUncertain) {
			return OperationalUncertainty.CRITICAL;
		}
		return switch (status) {
			case COLLECTED -> OperationalUncertainty.LOW;
			case PARTIAL -> OperationalUncertainty.MODERATE;
			case ABSENT, UNKNOWN, FAILED -> OperationalUncertainty.HIGH;
		};
	}
}
