package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityPostExecutionVerificationSkeletonTest {

	private final PostExecutionVerification verification =
			new PostExecutionVerification();

	@Test
	void shouldNotTreatExecutorSuccessAsVerified() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.SUCCESS,
								"exec-1",
								"execution success"
						),
						correlationWithoutVerificationEvidence(),
						false,
						false,
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionVerificationStatus.INCOMPLETE
		);
		assertThat(decision.verified()).isFalse();
	}

	@Test
	void shouldNotTreatExecutorSuccessAsConverged() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.SUCCESS,
								"exec-1",
								"execution success"
						),
						completeCorrelation(),
						true,
						false,
						false
				)
		);

		assertThat(decision.verified()).isTrue();
		assertThat(decision.converged()).isFalse();
	}

	@Test
	void shouldRequirePostExecutionVerificationEvidence() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.FAILURE,
								"exec-1",
								"execution failed"
						),
						correlationWithoutVerificationEvidence(),
						false,
						false,
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionVerificationStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionVerificationRejectionReason
						.MISSING_VERIFICATION_EVIDENCE
		);
	}

	@Test
	void shouldRequirePaymentConsistencyVerificationForPaymentImpactingExecution() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.SUCCESS,
								"exec-1",
								"execution success"
						),
						completeCorrelation(),
						false,
						true,
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionVerificationStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionVerificationRejectionReason
						.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
		);
	}

	@Test
	void shouldRejectContradictoryPostExecutionEvidence() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.SUCCESS,
								"exec-1",
								"execution success"
						),
						contradictoryCorrelation(),
						true,
						false,
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionVerificationStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionVerificationRejectionReason
						.CONTRADICTORY_POST_EXECUTION_EVIDENCE
		);
	}

	@Test
	void shouldRequireRollbackVerificationForRollbackTriggeredExecution() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.SUCCESS,
								"exec-1",
								"execution success"
						),
						completeCorrelation(),
						true,
						true,
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionVerificationStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionVerificationRejectionReason
						.ROLLBACK_VERIFICATION_REQUIRED
		);
	}

	@Test
	void shouldRejectUnknownExecutorResponse() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.UNKNOWN,
								"exec-1",
								"execution unknown"
						),
						completeCorrelation(),
						true,
						false,
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionVerificationStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionVerificationRejectionReason.EXECUTOR_RESPONSE_UNKNOWN
		);
	}

	@Test
	void shouldTreatVerificationAsExecutionInterpretationOnly() {
		PostExecutionVerificationDecision decision = verification.verify(
				requirement(
						new ExecutorResponse(
								ExecutorStatus.SUCCESS,
								"exec-1",
								"execution success"
						),
						completeCorrelation(),
						true,
						true,
						false
				)
		);

		assertThat(decision.executionInterpretationOnly()).isTrue();
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> verification.verify(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private PostExecutionVerificationRequirement requirement(
			ExecutorResponse executorResponse,
			EvidenceCorrelation evidenceCorrelation,
			boolean paymentConsistencyVerified,
			boolean rollbackTriggeredExecution,
			boolean rollbackVerified
	) {
		return new PostExecutionVerificationRequirement(
				executorResponse,
				new ExecutionRequirement(
						new ActionAdmissionDecision(
								true,
								ActionAdmissionScope.CANDIDATE,
								new ActionAdmissionRequirement(
										new ReliabilityRiskClassification(
												ReliabilityRiskLevel.LOW,
												List.of(
														ReliabilityRiskFactor
																.STABLE_CONVERGED_COMPLETE_EVIDENCE
												),
												ReliabilityRiskReason
														.STABLE_CONVERGED_COMPLETE_EVIDENCE_SUPPORTS_LOW_RISK
										),
										new HumanApprovalDecision(
												false,
												HumanApprovalScope.OPTIONAL,
												new HumanApprovalRequirement(
														false,
														false,
														false,
														false
												),
												List.of(
														HumanApprovalReason
																.AI_ONLY_APPROVAL_IS_NOT_ALLOWED
												)
										),
										new RecommendationEligibility(
												true,
												RecommendationScope.ADVISORY_WITH_VERIFICATION_REQUIREMENT,
												true,
												false,
												List.of(
														RecommendationRestriction
																.EXECUTION_AUTHORITY_PROHIBITED
												),
												List.of(
														RecommendationBoundaryReason
																.ASSESSMENT_RESULT_IS_NOT_RECOMMENDATION
												)
										),
										new ActionCommandEligibility(
												true,
												new ActionCommandRequirement(
														false,
														true,
														true
												),
												List.of(
														ActionCommandRestriction
																.RECOMMENDATION_IS_NOT_ACTION_COMMAND
												),
												List.of(
														ActionCommandBoundaryReason
																.RECOMMENDATION_IS_NOT_ACTION_COMMAND
												)
										),
										new ScenarioBindingDecision(
												ScenarioBindingStatus.BOUND,
												new ScenarioReference(
														"scenario-1",
														"portfolio-runtime",
														true,
														false
												),
												null
										),
										new RollbackVerificationBindingDecision(
												RollbackVerificationBindingStatus.BOUND,
												new RollbackReference(
														"rollback-1",
														"portfolio-runtime",
														true,
														false
												),
												new VerificationReference(
														"verification-1",
														"portfolio-runtime",
														true,
														false,
														true
												),
												null
										),
										new SafetyPolicyDecision(
												true,
												SafetyPolicyScope.STANDARD,
												new SafetyPolicyRequirement(
														assessResult(),
														new ReliabilityRiskClassification(
																ReliabilityRiskLevel.LOW,
																List.of(
																		ReliabilityRiskFactor
																				.STABLE_CONVERGED_COMPLETE_EVIDENCE
																),
																ReliabilityRiskReason
																		.STABLE_CONVERGED_COMPLETE_EVIDENCE_SUPPORTS_LOW_RISK
														),
														new HumanApprovalDecision(
																false,
																HumanApprovalScope.OPTIONAL,
																new HumanApprovalRequirement(
																		false,
																		false,
																		false,
																		false
																),
																List.of(
																		HumanApprovalReason
																				.AI_ONLY_APPROVAL_IS_NOT_ALLOWED
																)
														),
														new ScenarioBindingDecision(
																ScenarioBindingStatus.BOUND,
																new ScenarioReference(
																		"scenario-1",
																		"portfolio-runtime",
																		true,
																		false
																),
																null
														),
														new RollbackVerificationBindingDecision(
																RollbackVerificationBindingStatus.BOUND,
																new RollbackReference(
																		"rollback-1",
																		"portfolio-runtime",
																		true,
																		false
																),
																new VerificationReference(
																		"verification-1",
																		"portfolio-runtime",
																		true,
																		false,
																		true
																),
																null
														),
														true,
														true,
														false
												),
												null
										),
										false
								),
								null
						),
						true,
						true,
						true,
						true
				),
				evidenceCorrelation,
				paymentConsistencyVerified,
				rollbackTriggeredExecution,
				rollbackVerified
		);
	}

	private ReliabilityAssessmentResult assessResult() {
		return new ReliabilityAssessmentResult(
				RuntimeState.CONVERGED,
				List.of(ReliabilityAssessmentStage.EVIDENCE_CORRELATION),
				completeCorrelation(),
				null,
				null,
				null,
				OperationalUncertainty.LOW,
				null
		);
	}

	private EvidenceCorrelation completeCorrelation() {
		return EvidenceCorrelation.correlate(
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
	}

	private EvidenceCorrelation contradictoryCorrelation() {
		return EvidenceCorrelation.correlate(
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
	}

	private EvidenceCorrelation correlationWithoutVerificationEvidence() {
		return EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				false
		);
	}

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
