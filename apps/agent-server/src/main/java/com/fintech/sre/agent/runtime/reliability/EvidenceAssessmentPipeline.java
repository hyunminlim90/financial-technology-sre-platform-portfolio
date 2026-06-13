package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class EvidenceAssessmentPipeline {

	private final ReliabilityAssessmentOrchestrator assessmentOrchestrator;

	public EvidenceAssessmentPipeline(
			ReliabilityAssessmentOrchestrator assessmentOrchestrator
	) {
		this.assessmentOrchestrator = Objects.requireNonNull(
				assessmentOrchestrator,
				"assessmentOrchestrator must not be null"
		);
	}

	public EvidenceAssessmentPipelineResult run(
			EvidenceAssessmentPipelineInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				input.collectionResult().normalizedSignals(),
				input.collectionResult().contradictionMarkerPresent()
		);
		ReliabilityAssessmentResult assessmentResult = assessmentOrchestrator.assess(
				new ReliabilityAssessmentInput(
						runtimeState(input),
						correlation.signals(),
						correlation.contradictoryEvidence(),
						input.propagationSignal(),
						input.propagationActive(),
						input.rollbackRecentlyApplied(),
						input.convergenceWindow(),
						input.regressionSignals()
				)
		);

		return new EvidenceAssessmentPipelineResult(
				List.of(
						EvidenceAssessmentPipelineStage.EVIDENCE_COLLECTION,
						EvidenceAssessmentPipelineStage.EVIDENCE_CORRELATION,
						EvidenceAssessmentPipelineStage.RELIABILITY_ASSESSMENT
				),
				input.collectionResult(),
				correlation,
				assessmentResult,
				rejectionReason(input.collectionResult())
		);
	}

	private RuntimeState runtimeState(EvidenceAssessmentPipelineInput input) {
		if (input.collectionResult().status() == EvidenceCollectionStatus.FAILED
				|| input.collectionResult().status() == EvidenceCollectionStatus.UNKNOWN) {
			return RuntimeState.UNKNOWN;
		}
		return input.runtimeState();
	}

	private EvidenceAssessmentPipelineRejectionReason rejectionReason(
			EvidenceCollectionResult collectionResult
	) {
		if (collectionResult.rejectionReason() != null) {
			return EvidenceAssessmentPipelineRejectionReason.COLLECTION_REJECTED;
		}
		if (collectionResult.status() == EvidenceCollectionStatus.FAILED) {
			return EvidenceAssessmentPipelineRejectionReason.COLLECTION_FAILED;
		}
		return null;
	}
}
