package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceAssessmentPipelineTest {

	private final EvidenceAssessmentPipeline pipeline =
			new EvidenceAssessmentPipeline(
					new ReliabilityAssessmentOrchestrator(new VerificationGate())
			);

	@Test
	void shouldRemainReadOnlySemanticPipeline() {
		EvidenceAssessmentPipelineResult result = pipeline.run(input(
				collectedResult(
						List.of(signal(EvidenceSignalType.METRIC, "metric-1", "metric"))
				),
				RuntimeState.DEGRADED
		));

		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.exposesRawPayload()).isFalse();
	}

	@Test
	void shouldPreserveFixedCollectionToCorrelationToAssessmentOrder() {
		EvidenceAssessmentPipelineResult result = pipeline.run(input(
				collectedResult(
						List.of(signal(EvidenceSignalType.METRIC, "metric-1", "metric"))
				),
				RuntimeState.DEGRADED
		));

		assertThat(result.stages()).containsExactly(
				EvidenceAssessmentPipelineStage.EVIDENCE_COLLECTION,
				EvidenceAssessmentPipelineStage.EVIDENCE_CORRELATION,
				EvidenceAssessmentPipelineStage.RELIABILITY_ASSESSMENT
		);
	}

	@Test
	void shouldKeepAssessmentUncertaintyWhenCollectionFailed() {
		EvidenceAssessmentPipelineResult result = pipeline.run(input(
				failedResult(),
				RuntimeState.CONVERGED
		));

		assertThat(result.collectionResult().status()).isEqualTo(
				EvidenceCollectionStatus.FAILED
		);
		assertThat(result.assessmentResult().runtimeState()).isEqualTo(RuntimeState.UNKNOWN);
		assertThat(result.assessmentResult().overallRisk().ordinal())
				.isGreaterThanOrEqualTo(OperationalUncertainty.HIGH.ordinal());
		assertThat(result.rejectionReason()).isEqualTo(
				EvidenceAssessmentPipelineRejectionReason.COLLECTION_FAILED
		);
	}

	@Test
	void shouldNotAllowRawPayloadIntoPipeline() {
		EvidenceAssessmentPipelineInput pipelineInput = input(
				collectedResult(
						List.of(signal(EvidenceSignalType.LOG, "log-1", "log"))
				),
				RuntimeState.DEGRADED
		);

		assertThat(pipelineInput.exposesRawPayload()).isFalse();
		assertThat(pipeline.run(pipelineInput).exposesRawPayload()).isFalse();
	}

	@Test
	void shouldPropagateContradictoryCollectedEvidenceAsHighRiskUncertainty() {
		EvidenceAssessmentPipelineResult result = pipeline.run(input(
				contradictoryCollectedResult(),
				RuntimeState.DEGRADED
		));

		assertThat(result.evidenceCorrelation().contradictoryEvidence()).isTrue();
		assertThat(result.assessmentResult().overallRisk().ordinal())
				.isGreaterThanOrEqualTo(OperationalUncertainty.HIGH.ordinal());
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyWhenPaymentConsistencyEvidenceMissing() {
		EvidenceAssessmentPipelineResult result = pipeline.run(input(
				collectedResult(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1", "metric"),
								signal(EvidenceSignalType.LOG, "log-1", "log"),
								signal(EvidenceSignalType.TRACE, "trace-1", "trace"),
								signal(EvidenceSignalType.TIMELINE, "timeline-1", "timeline")
						)
				),
				RuntimeState.DEGRADED
		));

		assertThat(result.evidenceCorrelation().paymentSafetyUncertain()).isTrue();
		assertThat(result.assessmentResult().overallRisk())
				.isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldNotLeakAdapterOrVendorDetailIntoAssessmentLayer() {
		EvidenceAssessmentPipelineResult result = pipeline.run(input(
				collectedResult(
						List.of(signal(EvidenceSignalType.VERIFICATION, "verification-1", "ok"))
				),
				RuntimeState.VERIFYING
		));

		assertThat(result.assessmentResult().evidenceCorrelation().signals())
				.extracting(EvidenceSignal::signalId)
				.containsExactly("verification-1");
		assertThat(result.assessmentResult().evidenceCorrelation().signals())
				.allMatch(signal -> !signal.signalId().contains("prometheus"));
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceAssessmentPipelineInput pipelineInput = input(
				collectedResult(List.of()),
				RuntimeState.UNKNOWN
		);
		EvidenceAssessmentPipelineResult result = pipeline.run(pipelineInput);

		assertThat(pipelineInput.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullInput() {
		assertThatThrownBy(() -> pipeline.run(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private EvidenceAssessmentPipelineInput input(
			EvidenceCollectionResult collectionResult,
			RuntimeState runtimeState
	) {
		return new EvidenceAssessmentPipelineInput(
				collectionResult,
				runtimeState,
				PropagationSignal.CROSS_SERVICE,
				false,
				false,
				new ConvergenceWindow(
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				),
				List.of()
		);
	}

	private EvidenceCollectionResult collectedResult(List<EvidenceSignal> signals) {
		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				List.of(),
				signals,
				EvidenceCollectionStatus.COLLECTED,
				true,
				false,
				OperationalUncertainty.CRITICAL,
				null
		);
	}

	private EvidenceCollectionResult contradictoryCollectedResult() {
		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				List.of(),
				List.of(
						signal(EvidenceSignalType.LOG, "shared-1", "healthy"),
						signal(EvidenceSignalType.LOG, "shared-1", "degraded")
				),
				EvidenceCollectionStatus.PARTIAL,
				true,
				true,
				OperationalUncertainty.HIGH,
				null
		);
	}

	private EvidenceCollectionResult failedResult() {
		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				List.of(),
				List.of(),
				EvidenceCollectionStatus.FAILED,
				true,
				false,
				OperationalUncertainty.HIGH,
				null
		);
	}

	private EvidenceSignal signal(
			EvidenceSignalType type,
			String signalId,
			String summary
	) {
		return new EvidenceSignal(type, signalId, summary);
	}
}
