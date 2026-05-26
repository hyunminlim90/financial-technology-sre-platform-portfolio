package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceCorrelationSkeletonTest {

	@Test
	void shouldBeCompleteWhenAllCoreSignalsExist() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				false
		);

		assertThat(correlation.completeness()).isEqualTo(EvidenceCompleteness.COMPLETE);
		assertThat(correlation.operationalUncertainty()).isEqualTo(OperationalUncertainty.LOW);
	}

	@Test
	void shouldBePartialWhenOnlySomeCoreSignalsExist() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				false
		);

		assertThat(correlation.completeness()).isEqualTo(EvidenceCompleteness.PARTIAL);
		assertThat(correlation.operationalUncertainty())
				.isEqualTo(OperationalUncertainty.MODERATE);
	}

	@Test
	void shouldBeAbsentWhenNoCoreSignalsExist() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(),
				false
		);

		assertThat(correlation.completeness()).isEqualTo(EvidenceCompleteness.ABSENT);
		assertThat(correlation.verificationEvidencePresent()).isFalse();
	}

	@Test
	void shouldRejectConvergedAssessmentWithoutVerificationEvidence() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				false
		);

		EvidenceCorrelationDecision decision =
				EvidenceCorrelationDecision.evaluateForConverged(correlation);

		assertThat(decision.supportsConvergedAssessment()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						EvidenceCorrelationRejectionReason.MISSING_VERIFICATION_EVIDENCE
				);
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyWhenPaymentEvidenceMissing() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1")
				),
				false
		);

		EvidenceCorrelationDecision decision =
				EvidenceCorrelationDecision.evaluateForConverged(correlation);

		assertThat(correlation.paymentSafetyUncertain()).isTrue();
		assertThat(correlation.operationalUncertainty())
				.isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(decision.supportsConvergedAssessment()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						EvidenceCorrelationRejectionReason.PAYMENT_SAFETY_EVIDENCE_MISSING
				);
	}

	@Test
	void shouldTreatContradictoryEvidenceAsHighRiskUncertainty() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				true
		);

		EvidenceCorrelationDecision decision =
				EvidenceCorrelationDecision.evaluateForConverged(correlation);

		assertThat(correlation.contradictoryEvidence()).isTrue();
		assertThat(correlation.operationalUncertainty())
				.isEqualTo(OperationalUncertainty.HIGH);
		assertThat(decision.supportsConvergedAssessment()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(EvidenceCorrelationRejectionReason.HIGH_RISK_UNCERTAINTY);
	}

	@Test
	void shouldTreatEvidenceSignalsAsAppendOnlySemantic() {
		EvidenceSignal signal = signal(EvidenceSignalType.TIMELINE, "timeline-1");

		assertThat(signal.appendOnlySemantic()).isTrue();
	}

	@Test
	void shouldRemainAdvisoryAndNotExecutionTrigger() {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				false
		);

		assertThat(correlation.executionTrigger()).isFalse();
		assertThat(EvidenceCorrelationDecision.evaluateForConverged(correlation)
				.supportsConvergedAssessment()).isTrue();
	}

	@Test
	void shouldDefensivelyCopySignals() {
		List<EvidenceSignal> signals = new ArrayList<>(List.of(
				signal(EvidenceSignalType.METRIC, "metric-1")
		));

		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(signals, false);
		signals.add(signal(EvidenceSignalType.LOG, "log-1"));

		assertThat(correlation.signals()).hasSize(1);
		assertThatThrownBy(() -> correlation.signals().add(
				signal(EvidenceSignalType.TRACE, "trace-1")
		)).isInstanceOf(UnsupportedOperationException.class);
	}

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
