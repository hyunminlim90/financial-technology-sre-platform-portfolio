package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRiskClassificationSkeletonTest {

	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier classifier =
			new ReliabilityRiskClassifier();

	@Test
	void shouldClassifyPaymentSafetyUncertaintyAsAtLeastHigh() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(
						RuntimeState.VERIFIED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.TRACE, "trace-1"),
								signal(EvidenceSignalType.TIMELINE, "timeline-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1")
						),
						false,
						false,
						List.of()
				)
		);

		assertThat(classification.level()).isIn(
				ReliabilityRiskLevel.HIGH,
				ReliabilityRiskLevel.CRITICAL
		);
		assertThat(classification.factors())
				.contains(ReliabilityRiskFactor.PAYMENT_SAFETY_UNCERTAINTY);
	}

	@Test
	void shouldClassifyContradictoryEvidenceAsAtLeastHigh() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						true,
						false,
						List.of()
				)
		);

		assertThat(classification.level()).isEqualTo(ReliabilityRiskLevel.HIGH);
		assertThat(classification.factors())
				.contains(ReliabilityRiskFactor.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldClassifyActivePropagationWithPartialEvidenceAsHighOrAbove() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(
						RuntimeState.VERIFIED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false,
						true,
						List.of()
				)
		);

		assertThat(classification.level()).isEqualTo(ReliabilityRiskLevel.HIGH);
		assertThat(classification.factors())
				.contains(
						ReliabilityRiskFactor.ACTIVE_PROPAGATION_WITH_PARTIAL_EVIDENCE
				);
	}

	@Test
	void shouldClassifyFailedStateAsCritical() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(RuntimeState.FAILED, completeEvidence(), false, false, List.of())
		);

		assertThat(classification.level()).isEqualTo(ReliabilityRiskLevel.CRITICAL);
		assertThat(classification.reason())
				.isEqualTo(ReliabilityRiskReason.FAILED_STATE_IS_CRITICAL);
	}

	@Test
	void shouldTreatStableConvergedCompleteEvidenceAsLowCandidate() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(classification.level()).isEqualTo(ReliabilityRiskLevel.LOW);
		assertThat(classification.factors())
				.contains(ReliabilityRiskFactor.STABLE_CONVERGED_COMPLETE_EVIDENCE);
	}

	@Test
	void shouldNotAllowPartialEvidenceToBecomeLow() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(
						RuntimeState.CONVERGED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false,
						false,
						List.of()
				)
		);

		assertThat(classification.level()).isNotEqualTo(ReliabilityRiskLevel.LOW);
		assertThat(classification.factors())
				.contains(ReliabilityRiskFactor.PARTIAL_EVIDENCE);
	}

	@Test
	void shouldClassifyUnknownOrAbsentEvidenceAsAtLeastMedium() {
		ReliabilityRiskClassification unknownStateClassification = classifier.classify(
				assess(
						RuntimeState.UNKNOWN,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);
		ReliabilityRiskClassification absentEvidenceClassification =
				classifier.classify(
						assess(RuntimeState.VERIFIED, List.of(), false, false, List.of())
				);

		assertThat(unknownStateClassification.level()).isIn(
				ReliabilityRiskLevel.MEDIUM,
				ReliabilityRiskLevel.HIGH,
				ReliabilityRiskLevel.CRITICAL
		);
		assertThat(absentEvidenceClassification.level()).isIn(
				ReliabilityRiskLevel.MEDIUM,
				ReliabilityRiskLevel.HIGH,
				ReliabilityRiskLevel.CRITICAL
		);
	}

	@Test
	void shouldRemainSemanticClassificationOnly() {
		ReliabilityRiskClassification classification = classifier.classify(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(classification.semanticOnly()).isTrue();
		assertThat(classification.executionTrigger()).isFalse();
	}

	@Test
	void shouldRejectNullAssessmentResult() {
		assertThatThrownBy(() -> classifier.classify(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessmentResult must not be null");
	}

	private ReliabilityAssessmentResult assess(
			RuntimeState runtimeState,
			List<EvidenceSignal> evidenceSignals,
			boolean contradictoryEvidence,
			boolean propagationActive,
			List<RegressionSignal> regressionSignals
	) {
		return orchestrator.assess(new ReliabilityAssessmentInput(
				runtimeState,
				evidenceSignals,
				contradictoryEvidence,
				PropagationSignal.CROSS_SERVICE,
				propagationActive,
				false,
				new ConvergenceWindow(
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				),
				regressionSignals
		));
	}

	private List<EvidenceSignal> completeEvidence() {
		return List.of(
				signal(EvidenceSignalType.METRIC, "metric-1"),
				signal(EvidenceSignalType.LOG, "log-1"),
				signal(EvidenceSignalType.TRACE, "trace-1"),
				signal(EvidenceSignalType.TIMELINE, "timeline-1"),
				signal(EvidenceSignalType.VERIFICATION, "verification-1"),
				signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
		);
	}

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
