package com.fintech.sre.agent.runtime.reliability;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public record EvidenceCorrelation(
		List<EvidenceSignal> signals,
		EvidenceCompleteness completeness,
		boolean verificationEvidencePresent,
		boolean paymentSafetyEvidencePresent,
		boolean contradictoryEvidence,
		OperationalUncertainty operationalUncertainty
) {
	private static final EnumSet<EvidenceSignalType> CORE_TYPES = EnumSet.of(
			EvidenceSignalType.METRIC,
			EvidenceSignalType.LOG,
			EvidenceSignalType.TRACE,
			EvidenceSignalType.TIMELINE
	);

	public EvidenceCorrelation {
		Objects.requireNonNull(signals, "signals must not be null");
		Objects.requireNonNull(completeness, "completeness must not be null");
		Objects.requireNonNull(
				operationalUncertainty,
				"operationalUncertainty must not be null"
		);
		signals = List.copyOf(signals);
	}

	public static EvidenceCorrelation correlate(
			List<EvidenceSignal> signals,
			boolean contradictoryEvidence
	) {
		Objects.requireNonNull(signals, "signals must not be null");

		List<EvidenceSignal> copiedSignals = List.copyOf(signals);
		EnumSet<EvidenceSignalType> presentTypes = EnumSet.noneOf(EvidenceSignalType.class);
		for (EvidenceSignal signal : copiedSignals) {
			presentTypes.add(signal.type());
		}

		boolean verificationEvidencePresent =
				presentTypes.contains(EvidenceSignalType.VERIFICATION);
		boolean paymentSafetyEvidencePresent =
				presentTypes.contains(EvidenceSignalType.PAYMENT_SAFETY);

		EvidenceCompleteness completeness = completenessOf(presentTypes);
		OperationalUncertainty operationalUncertainty = uncertaintyOf(
				completeness,
				paymentSafetyEvidencePresent,
				contradictoryEvidence
		);

		return new EvidenceCorrelation(
				copiedSignals,
				completeness,
				verificationEvidencePresent,
				paymentSafetyEvidencePresent,
				contradictoryEvidence,
				operationalUncertainty
		);
	}

	private static EvidenceCompleteness completenessOf(
			EnumSet<EvidenceSignalType> presentTypes
	) {
		int matchedCoreSignals = 0;
		for (EvidenceSignalType coreType : CORE_TYPES) {
			if (presentTypes.contains(coreType)) {
				matchedCoreSignals++;
			}
		}

		if (matchedCoreSignals == 0) {
			return EvidenceCompleteness.ABSENT;
		}
		if (matchedCoreSignals == CORE_TYPES.size()) {
			return EvidenceCompleteness.COMPLETE;
		}
		return EvidenceCompleteness.PARTIAL;
	}

	private static OperationalUncertainty uncertaintyOf(
			EvidenceCompleteness completeness,
			boolean paymentSafetyEvidencePresent,
			boolean contradictoryEvidence
	) {
		if (contradictoryEvidence) {
			return OperationalUncertainty.HIGH;
		}
		if (!paymentSafetyEvidencePresent) {
			return OperationalUncertainty.CRITICAL;
		}
		return switch (completeness) {
			case COMPLETE -> OperationalUncertainty.LOW;
			case PARTIAL -> OperationalUncertainty.MODERATE;
			case ABSENT -> OperationalUncertainty.HIGH;
		};
	}

	public boolean executionTrigger() {
		return false;
	}

	public boolean paymentSafetyUncertain() {
		return !paymentSafetyEvidencePresent;
	}
}
