package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class ObservableReliabilityRuntimePipeline {

	private final EvidenceCollectionOrchestrator evidenceCollectionOrchestrator;
	private final EvidenceAssessmentPipeline evidenceAssessmentPipeline;
	private final AssessmentLifecyclePipeline assessmentLifecyclePipeline;
	private final ReliabilityLifecycleSummaryResource lifecycleSummaryResource;

	public ObservableReliabilityRuntimePipeline(
			EvidenceCollectionOrchestrator evidenceCollectionOrchestrator,
			EvidenceAssessmentPipeline evidenceAssessmentPipeline,
			AssessmentLifecyclePipeline assessmentLifecyclePipeline,
			ReliabilityLifecycleSummaryResource lifecycleSummaryResource
	) {
		this.evidenceCollectionOrchestrator = Objects.requireNonNull(
				evidenceCollectionOrchestrator,
				"evidenceCollectionOrchestrator must not be null"
		);
		this.evidenceAssessmentPipeline = Objects.requireNonNull(
				evidenceAssessmentPipeline,
				"evidenceAssessmentPipeline must not be null"
		);
		this.assessmentLifecyclePipeline = Objects.requireNonNull(
				assessmentLifecyclePipeline,
				"assessmentLifecyclePipeline must not be null"
		);
		this.lifecycleSummaryResource = Objects.requireNonNull(
				lifecycleSummaryResource,
				"lifecycleSummaryResource must not be null"
		);
	}

	public ObservableReliabilityRuntimeResult run(
			ObservableReliabilityRuntimeInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		EvidenceCollectionResult collectionResult = evidenceCollectionOrchestrator.collect(
				new EvidenceCollectionRequest(input.adapters(), input.queries())
		);
		EvidenceAssessmentPipelineResult assessmentPipelineResult =
				evidenceAssessmentPipeline.run(
						new EvidenceAssessmentPipelineInput(
								collectionResult,
								input.runtimeState(),
								input.propagationSignal(),
								input.propagationActive(),
								input.rollbackRecentlyApplied(),
								input.convergenceWindow(),
								input.regressionSignals()
						)
				);
		AssessmentLifecyclePipelineResult assessmentLifecyclePipelineResult =
				assessmentLifecyclePipeline.run(
						new AssessmentLifecyclePipelineInput(
								assessmentPipelineResult,
								input.scenarioReference(),
								input.rollbackReference(),
								input.verificationReference(),
								input.approvalProvided(),
								input.explicitApprovalProvided(),
								input.paymentSafetyAction(),
								input.unrestrictedRequested(),
								input.explicitExecutionAuthorized(),
								input.approvalCompleted(),
								input.rollbackReviewCompleted(),
								input.verificationReviewCompleted(),
								input.lifecycleAuditDecision()
						)
				);
		ReliabilityLifecycleSummaryResponse summaryResponse =
				lifecycleSummaryResource.view(
						assessmentLifecyclePipelineResult.lifecycleSummary()
				);

		return new ObservableReliabilityRuntimeResult(
				List.of(
						ObservableReliabilityRuntimeStage.EVIDENCE_COLLECTION,
						ObservableReliabilityRuntimeStage.EVIDENCE_ASSESSMENT,
						ObservableReliabilityRuntimeStage.ASSESSMENT_LIFECYCLE,
						ObservableReliabilityRuntimeStage.LIFECYCLE_SUMMARY_RESOURCE
				),
				collectionResult,
				assessmentPipelineResult,
				assessmentLifecyclePipelineResult,
				summaryResponse,
				rejectionReason(
						collectionResult,
						assessmentPipelineResult,
						assessmentLifecyclePipelineResult
				)
		);
	}

	private ObservableReliabilityRuntimeRejectionReason rejectionReason(
			EvidenceCollectionResult collectionResult,
			EvidenceAssessmentPipelineResult assessmentPipelineResult,
			AssessmentLifecyclePipelineResult assessmentLifecyclePipelineResult
	) {
		if (collectionResult.rejectionReason() != null) {
			return ObservableReliabilityRuntimeRejectionReason
					.EVIDENCE_COLLECTION_REJECTED;
		}
		if (assessmentPipelineResult.rejectionReason() != null) {
			return ObservableReliabilityRuntimeRejectionReason
					.EVIDENCE_ASSESSMENT_REJECTED;
		}
		if (assessmentLifecyclePipelineResult.rejectionReason() != null) {
			return ObservableReliabilityRuntimeRejectionReason
					.ASSESSMENT_LIFECYCLE_REJECTED;
		}
		return null;
	}
}
