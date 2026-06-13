package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class AssessmentLifecyclePipeline {

	private final ReliabilityRiskClassifier riskClassifier;
	private final HumanApprovalPolicy humanApprovalPolicy;
	private final ReliabilityRecommendationBoundary recommendationBoundary;
	private final ActionCommandBoundary actionCommandBoundary;
	private final ScenarioBinding scenarioBinding;
	private final RollbackVerificationBinding rollbackVerificationBinding;
	private final SafetyPolicyGate safetyPolicyGate;
	private final ActionAdmissionGate actionAdmissionGate;
	private final ExecutionBoundary executionBoundary;
	private final ReliabilityExecutorContract executorContract;
	private final ExecutionReadinessGate executionReadinessGate;
	private final ReliabilityLifecycleSummaryBuilder lifecycleSummaryBuilder;

	public AssessmentLifecyclePipeline(
			ReliabilityRiskClassifier riskClassifier,
			HumanApprovalPolicy humanApprovalPolicy,
			ReliabilityRecommendationBoundary recommendationBoundary,
			ActionCommandBoundary actionCommandBoundary,
			ScenarioBinding scenarioBinding,
			RollbackVerificationBinding rollbackVerificationBinding,
			SafetyPolicyGate safetyPolicyGate,
			ActionAdmissionGate actionAdmissionGate,
			ExecutionBoundary executionBoundary,
			ReliabilityExecutorContract executorContract,
			ExecutionReadinessGate executionReadinessGate,
			ReliabilityLifecycleSummaryBuilder lifecycleSummaryBuilder
	) {
		this.riskClassifier = Objects.requireNonNull(
				riskClassifier,
				"riskClassifier must not be null"
		);
		this.humanApprovalPolicy = Objects.requireNonNull(
				humanApprovalPolicy,
				"humanApprovalPolicy must not be null"
		);
		this.recommendationBoundary = Objects.requireNonNull(
				recommendationBoundary,
				"recommendationBoundary must not be null"
		);
		this.actionCommandBoundary = Objects.requireNonNull(
				actionCommandBoundary,
				"actionCommandBoundary must not be null"
		);
		this.scenarioBinding = Objects.requireNonNull(
				scenarioBinding,
				"scenarioBinding must not be null"
		);
		this.rollbackVerificationBinding = Objects.requireNonNull(
				rollbackVerificationBinding,
				"rollbackVerificationBinding must not be null"
		);
		this.safetyPolicyGate = Objects.requireNonNull(
				safetyPolicyGate,
				"safetyPolicyGate must not be null"
		);
		this.actionAdmissionGate = Objects.requireNonNull(
				actionAdmissionGate,
				"actionAdmissionGate must not be null"
		);
		this.executionBoundary = Objects.requireNonNull(
				executionBoundary,
				"executionBoundary must not be null"
		);
		this.executorContract = Objects.requireNonNull(
				executorContract,
				"executorContract must not be null"
		);
		this.executionReadinessGate = Objects.requireNonNull(
				executionReadinessGate,
				"executionReadinessGate must not be null"
		);
		this.lifecycleSummaryBuilder = Objects.requireNonNull(
				lifecycleSummaryBuilder,
				"lifecycleSummaryBuilder must not be null"
		);
	}

	public AssessmentLifecyclePipelineResult run(
			AssessmentLifecyclePipelineInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		ReliabilityAssessmentResult assessmentResult = input
				.assessmentPipelineResult()
				.assessmentResult();
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = humanApprovalPolicy.evaluate(
				assessmentResult,
				riskClassification
		);
		RecommendationEligibility recommendationEligibility =
				recommendationBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalDecision
				);
		ActionCommandEligibility actionCommandEligibility =
				actionCommandBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						recommendationEligibility
				);
		ScenarioBindingDecision scenarioBindingDecision = scenarioBinding.bind(
				input.scenarioReference()
		);
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				rollbackVerificationBinding.bind(
						input.rollbackReference(),
						input.verificationReference(),
						input.paymentSafetyAction()
				);
		SafetyPolicyDecision safetyPolicyDecision = safetyPolicyGate.evaluate(
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						input.approvalProvided(),
						input.explicitApprovalProvided(),
						input.paymentSafetyAction()
				)
		);
		ActionAdmissionDecision actionAdmissionDecision = actionAdmissionGate.evaluate(
				new ActionAdmissionRequirement(
						riskClassification,
						humanApprovalDecision,
						recommendationEligibility,
						actionCommandEligibility,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						safetyPolicyDecision,
						input.unrestrictedRequested()
				)
		);

		ExecutionRequirement executionRequirement = new ExecutionRequirement(
				actionAdmissionDecision,
				input.explicitExecutionAuthorized(),
				input.approvalCompleted(),
				input.rollbackReviewCompleted(),
				input.verificationReviewCompleted()
		);
		ExecutionBoundaryDecision executionBoundaryDecision =
				executionBoundary.evaluate(executionRequirement);
		ExecutionPlan executionPlan = executorContract.plan(new ExecutionIntent(
				executionBoundaryDecision,
				"rollback-plan-reference",
				"verification-plan-reference",
				input.paymentSafetyAction(),
				input.verificationReference() != null
						&& input.verificationReference()
								.paymentConsistencyVerification(),
				false,
				input.explicitExecutionAuthorized()
		));
		ExecutionReadinessDecision executionReadinessDecision =
				executionReadinessGate.evaluate(new ExecutionReadinessRequirement(
						executionBoundaryDecision,
						executionPlan,
						new ExecutionAuditDecision(
								ExecutionAuditTrail.empty(),
								input.lifecycleAuditDecision().lifecycleTrustworthy()
										? ExecutionAuditIntegrity.VERIFIED
										: ExecutionAuditIntegrity.INCOMPLETE
						)
				));

		ReliabilityLifecycleSummary lifecycleSummary = lifecycleSummaryBuilder.build(
				syntheticLifecycleResult(
						assessmentResult,
						riskClassification,
						actionAdmissionDecision,
						executionReadinessDecision
				),
				input.lifecycleAuditDecision()
		);

		return new AssessmentLifecyclePipelineResult(
				List.of(
						AssessmentLifecyclePipelineStage.RISK_CLASSIFICATION,
						AssessmentLifecyclePipelineStage.HUMAN_APPROVAL_POLICY,
						AssessmentLifecyclePipelineStage.RECOMMENDATION_BOUNDARY,
						AssessmentLifecyclePipelineStage.ACTION_COMMAND_BOUNDARY,
						AssessmentLifecyclePipelineStage.SCENARIO_BINDING,
						AssessmentLifecyclePipelineStage.ROLLBACK_VERIFICATION_BINDING,
						AssessmentLifecyclePipelineStage.SAFETY_POLICY_GATE,
						AssessmentLifecyclePipelineStage.ACTION_ADMISSION,
						AssessmentLifecyclePipelineStage.EXECUTION_READINESS,
						AssessmentLifecyclePipelineStage.LIFECYCLE_SUMMARY
				),
				input.assessmentPipelineResult(),
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility,
				actionCommandEligibility,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				safetyPolicyDecision,
				actionAdmissionDecision,
				executionReadinessDecision,
				lifecycleSummary,
				rejectionReason(input, actionAdmissionDecision, executionReadinessDecision)
		);
	}

	private AssessmentLifecyclePipelineRejectionReason rejectionReason(
			AssessmentLifecyclePipelineInput input,
			ActionAdmissionDecision actionAdmissionDecision,
			ExecutionReadinessDecision executionReadinessDecision
	) {
		if (input.assessmentPipelineResult().rejectionReason() != null) {
			return AssessmentLifecyclePipelineRejectionReason
					.ASSESSMENT_PIPELINE_REJECTED;
		}
		if (!actionAdmissionDecision.admitted()) {
			return AssessmentLifecyclePipelineRejectionReason
					.ACTION_ADMISSION_REJECTED;
		}
		if (!executionReadinessDecision.ready()) {
			return AssessmentLifecyclePipelineRejectionReason
					.EXECUTION_READINESS_REJECTED;
		}
		return null;
	}

	private ReliabilityLifecycleResult syntheticLifecycleResult(
			ReliabilityAssessmentResult assessmentResult,
			ReliabilityRiskClassification riskClassification,
			ActionAdmissionDecision actionAdmissionDecision,
			ExecutionReadinessDecision executionReadinessDecision
	) {
		ExecutorResponse executorResponse = new ExecutorResponse(
				ExecutorStatus.UNKNOWN,
				"synthetic-no-executor",
				"No executor invocation is performed in assessment lifecycle pipeline."
		);
		PostExecutionVerificationDecision verificationDecision =
				new PostExecutionVerificationDecision(
						PostExecutionVerificationStatus.REJECTED,
						new PostExecutionVerificationRequirement(
								executorResponse,
								executionReadinessDecision.requirement()
										.executionBoundaryDecision()
										.requirement(),
								assessmentResult.evidenceCorrelation(),
								false,
								false,
								false
						),
						PostExecutionVerificationRejectionReason
								.EXECUTOR_RESPONSE_UNKNOWN
				);
		PostExecutionConvergenceDecision convergenceDecision =
				new PostExecutionConvergenceDecision(
						PostExecutionConvergenceStatus.INCOMPLETE,
						new PostExecutionConvergenceRequirement(
								verificationDecision,
								new ConvergenceWindow(
										java.time.Duration.ofMinutes(5),
										java.time.Duration.ZERO
								),
								PropagationSignal.CROSS_SERVICE,
								false
						),
						PostExecutionConvergenceRejectionReason
								.VERIFICATION_NOT_SUFFICIENT
				);
		PostExecutionRegressionDecision regressionDecision = syntheticRegressionDecision(
				assessmentResult,
				convergenceDecision
		);

		return new ReliabilityLifecycleResult(
				List.of(
						ReliabilityLifecycleStage.PRE_EXECUTION_ASSESSMENT,
						ReliabilityLifecycleStage.ACTION_ADMISSION,
						ReliabilityLifecycleStage.EXECUTION_READINESS,
						ReliabilityLifecycleStage.EXECUTOR_RESPONSE,
						ReliabilityLifecycleStage.POST_EXECUTION_VERIFICATION,
						ReliabilityLifecycleStage.POST_EXECUTION_CONVERGENCE,
						ReliabilityLifecycleStage.POST_EXECUTION_REGRESSION
				),
				assessmentResult,
				actionAdmissionDecision,
				executionReadinessDecision,
				executorResponse,
				verificationDecision,
				convergenceDecision,
				regressionDecision,
				RuntimeState.UNSTABLE,
				max(
						assessmentResult.overallRisk(),
						uncertaintyOf(riskClassification.level())
				),
				!actionAdmissionDecision.admitted()
						? ReliabilityLifecycleRejectionReason.ACTION_ADMISSION_REJECTED
						: ReliabilityLifecycleRejectionReason.EXECUTOR_RESPONSE_UNKNOWN
		);
	}

	private PostExecutionRegressionDecision syntheticRegressionDecision(
			ReliabilityAssessmentResult assessmentResult,
			PostExecutionConvergenceDecision convergenceDecision
	) {
		List<RegressionSignal> propagatedSignals =
				assessmentRegressionSignals(assessmentResult);
		if (!propagatedSignals.isEmpty()) {
			return new PostExecutionRegressionDecision(
					PostExecutionRegressionStatus.DETECTED,
					new PostExecutionRegressionRequirement(
							convergenceDecision,
							propagatedSignals
					),
					regressionSeverity(propagatedSignals),
					regressionUncertainty(
							propagatedSignals,
							assessmentResult.overallRisk()
					),
					null
			);
		}
		return new PostExecutionRegressionDecision(
				PostExecutionRegressionStatus.REJECTED,
				new PostExecutionRegressionRequirement(
						convergenceDecision,
						List.of()
				),
				RegressionSeverity.MODERATE,
				assessmentResult.overallRisk(),
				PostExecutionRegressionRejectionReason.NO_REGRESSION_SIGNALS
		);
	}

	private List<RegressionSignal> assessmentRegressionSignals(
			ReliabilityAssessmentResult assessmentResult
	) {
		if (assessmentResult.regressionDecision() != null
				&& assessmentResult.regressionDecision().assessment().hasSignals()) {
			return assessmentResult.regressionDecision().assessment().signals();
		}
		if (assessmentResult.evidenceCorrelation().contradictoryEvidence()) {
			return List.of(new RegressionSignal(
					RegressionSignalType.CONTRADICTORY_EVIDENCE,
					"assessment-derived-contradiction",
					"Assessment contradiction propagates to lifecycle regression candidate."
			));
		}
		return List.of();
	}

	private RegressionSeverity regressionSeverity(
			List<RegressionSignal> signals
	) {
		return signals.stream().anyMatch(signal -> signal.type()
				== RegressionSignalType.PAYMENT_INCONSISTENCY
				|| signal.type() == RegressionSignalType.PAYMENT_SAFETY
				|| signal.type() == RegressionSignalType.PROPAGATION_REACTIVATED
				|| signal.type() == RegressionSignalType.CONTRADICTORY_EVIDENCE)
						? RegressionSeverity.HIGH
						: RegressionSeverity.MODERATE;
	}

	private OperationalUncertainty regressionUncertainty(
			List<RegressionSignal> signals,
			OperationalUncertainty assessmentRisk
	) {
		if (signals.stream().anyMatch(signal -> signal.type()
				== RegressionSignalType.PAYMENT_INCONSISTENCY)) {
			return OperationalUncertainty.CRITICAL;
		}
		if (signals.stream().anyMatch(signal -> signal.type()
				== RegressionSignalType.PAYMENT_SAFETY
				|| signal.type() == RegressionSignalType.PROPAGATION_REACTIVATED
				|| signal.type() == RegressionSignalType.CONTRADICTORY_EVIDENCE)) {
			return max(OperationalUncertainty.HIGH, assessmentRisk);
		}
		return assessmentRisk;
	}

	private OperationalUncertainty uncertaintyOf(ReliabilityRiskLevel level) {
		return switch (level) {
			case LOW -> OperationalUncertainty.LOW;
			case MEDIUM -> OperationalUncertainty.MODERATE;
			case HIGH -> OperationalUncertainty.HIGH;
			case CRITICAL -> OperationalUncertainty.CRITICAL;
		};
	}

	private OperationalUncertainty max(
			OperationalUncertainty left,
			OperationalUncertainty right
	) {
		return left.ordinal() >= right.ordinal() ? left : right;
	}
}
