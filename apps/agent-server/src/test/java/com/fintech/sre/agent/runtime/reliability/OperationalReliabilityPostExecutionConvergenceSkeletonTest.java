package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityPostExecutionConvergenceSkeletonTest {

	private final PostExecutionVerification verification =
			new PostExecutionVerification();
	private final PostExecutionConvergence convergence =
			new PostExecutionConvergence();

	@Test
	void shouldNotTreatPostExecutionVerificationSuccessAsConverged() {
		PostExecutionVerificationDecision verificationDecision = verifiedDecision(
				completeCorrelation(),
				true,
				false,
				false
		);

		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verificationDecision,
						Duration.ofMinutes(10),
						Duration.ofMinutes(2),
						false
				)
		);

		assertThat(verificationDecision.verified()).isTrue();
		assertThat(decision.converged()).isFalse();
		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.REJECTED
		);
	}

	@Test
	void shouldRejectWithoutStabilizationWindow() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verifiedDecision(completeCorrelation(), true, false, false),
						Duration.ofMinutes(10),
						Duration.ofMinutes(1),
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionConvergenceRejectionReason
						.STABILIZATION_WINDOW_NOT_SATISFIED
		);
	}

	@Test
	void shouldRejectWhenPropagationIsStillActive() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verifiedDecision(completeCorrelation(), true, false, false),
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						true
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionConvergenceRejectionReason.PROPAGATION_STILL_ACTIVE
		);
	}

	@Test
	void shouldRequirePaymentConsistencyForPaymentImpactingExecution() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verifiedDecision(completeCorrelation(), false, false, false),
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.INCOMPLETE
		);
	}

	@Test
	void shouldRejectContradictoryPostExecutionEvidence() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verification.verify(baseRequirement(
								new ExecutorResponse(
										ExecutorStatus.SUCCESS,
										"exec-1",
										"execution success"
								),
								contradictoryCorrelation(),
								true,
								false,
								false
						)),
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.INCOMPLETE
		);
	}

	@Test
	void shouldRequireRollbackVerificationForRollbackTriggeredExecution() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verification.verify(baseRequirement(
								new ExecutorResponse(
										ExecutorStatus.SUCCESS,
										"exec-1",
										"execution success"
								),
								completeCorrelation(),
								true,
								true,
								false
						)),
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.INCOMPLETE
		);
	}

	@Test
	void shouldRejectUnknownExecutorResponse() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verification.verify(baseRequirement(
								new ExecutorResponse(
										ExecutorStatus.UNKNOWN,
										"exec-1",
										"execution unknown"
								),
								completeCorrelation(),
								true,
								false,
								false
						)),
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						false
				)
		);

		assertThat(decision.status()).isEqualTo(
				PostExecutionConvergenceStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionConvergenceRejectionReason.UNKNOWN_EXECUTOR_RESPONSE
		);
	}

	@Test
	void shouldTreatConvergenceAsTemporalSemanticStabilityOnly() {
		PostExecutionConvergenceDecision decision = convergence.assess(
				requirement(
						verifiedDecision(completeCorrelation(), true, false, false),
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						false
				)
		);

		assertThat(decision.converged()).isTrue();
		assertThat(decision.executionResultOnly()).isFalse();
		assertThat(decision.temporalSemanticStabilityOnly()).isTrue();
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> convergence.assess(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private PostExecutionConvergenceRequirement requirement(
			PostExecutionVerificationDecision verificationDecision,
			Duration required,
			Duration observed,
			boolean propagationActive
	) {
		return new PostExecutionConvergenceRequirement(
				verificationDecision,
				new ConvergenceWindow(required, observed),
				PropagationSignal.CROSS_SERVICE,
				propagationActive
		);
	}

	private PostExecutionVerificationDecision verifiedDecision(
			EvidenceCorrelation evidenceCorrelation,
			boolean paymentConsistencyVerified,
			boolean rollbackTriggeredExecution,
			boolean rollbackVerified
	) {
		return verification.verify(baseRequirement(
				new ExecutorResponse(
						ExecutorStatus.SUCCESS,
						"exec-1",
						"execution success"
				),
				evidenceCorrelation,
				paymentConsistencyVerified,
				rollbackTriggeredExecution,
				rollbackVerified
		));
	}

	private PostExecutionVerificationRequirement baseRequirement(
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
														assessResult(evidenceCorrelation),
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

	private ReliabilityAssessmentResult assessResult(
			EvidenceCorrelation evidenceCorrelation
	) {
		return new ReliabilityAssessmentResult(
				RuntimeState.CONVERGED,
				List.of(ReliabilityAssessmentStage.EVIDENCE_CORRELATION),
				evidenceCorrelation,
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

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
