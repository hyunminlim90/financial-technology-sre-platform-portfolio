package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityPostExecutionRegressionSkeletonTest {

	private final PostExecutionRegression regression =
			new PostExecutionRegression();

	@Test
	void shouldTreatPostExecutionConvergedAsNonImmutableTruth() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						convergedDecision(),
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.requirement().postExecutionConverged()).isTrue();
	}

	@Test
	void shouldTreatDegradationEvidenceAfterConvergenceAsRegressionCandidate() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						convergedDecision(),
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.MODERATE);
	}

	@Test
	void shouldTreatPaymentInconsistencyAsHighOrCriticalRegressionCandidate() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						convergedDecision(),
						List.of(signal(
								RegressionSignalType.PAYMENT_INCONSISTENCY,
								"payment-inconsistency-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.HIGH);
		assertThat(decision.uncertainty()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldTreatPropagationReactivationAsConvergenceInvalidationCandidate() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						convergedDecision(),
						List.of(signal(
								RegressionSignalType.PROPAGATION_REACTIVATED,
								"propagation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.HIGH);
	}

	@Test
	void shouldTreatContradictoryPostConvergenceEvidenceAsRegressionCandidate() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						convergedDecision(),
						List.of(signal(
								RegressionSignalType.CONTRADICTORY_EVIDENCE,
								"contradiction-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.HIGH);
	}

	@Test
	void shouldRemainSemanticDetectionOnly() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						convergedDecision(),
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.semanticDetectionOnly()).isTrue();
	}

	@Test
	void shouldRequireReverificationAndReconvergenceAfterRegression() {
		PostExecutionRegressionRequirement requirement = requirement(
				convergedDecision(),
				List.of(signal(
						RegressionSignalType.SERVICE_DEGRADATION,
						"degradation-1"
				))
		);

		PostExecutionRegressionDecision decision = regression.detect(requirement);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(requirement.requiresReverificationAndReconvergence()).isTrue();
	}

	@Test
	void shouldRejectNonConvergedPostExecutionRegression() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(
						nonConvergedDecision(),
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionRegressionRejectionReason.NOT_POST_EXECUTION_CONVERGED
		);
	}

	@Test
	void shouldRejectWhenNoRegressionSignalsExist() {
		PostExecutionRegressionDecision decision = regression.detect(
				requirement(convergedDecision(), List.of())
		);

		assertThat(decision.regressionDetected()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				PostExecutionRegressionRejectionReason.NO_REGRESSION_SIGNALS
		);
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> regression.detect(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private PostExecutionRegressionRequirement requirement(
			PostExecutionConvergenceDecision convergenceDecision,
			List<RegressionSignal> signals
	) {
		return new PostExecutionRegressionRequirement(convergenceDecision, signals);
	}

	private PostExecutionConvergenceDecision convergedDecision() {
		return new PostExecutionConvergenceDecision(
				PostExecutionConvergenceStatus.CONVERGED,
				new PostExecutionConvergenceRequirement(
						new PostExecutionVerificationDecision(
								PostExecutionVerificationStatus.VERIFIED,
								baseVerificationRequirement(),
								null
						),
						new ConvergenceWindow(
								Duration.ofMinutes(5),
								Duration.ofMinutes(5)
						),
						PropagationSignal.CROSS_SERVICE,
						false
				),
				null
		);
	}

	private PostExecutionConvergenceDecision nonConvergedDecision() {
		return new PostExecutionConvergenceDecision(
				PostExecutionConvergenceStatus.INCOMPLETE,
				new PostExecutionConvergenceRequirement(
						new PostExecutionVerificationDecision(
								PostExecutionVerificationStatus.VERIFIED,
								baseVerificationRequirement(),
								null
						),
						new ConvergenceWindow(
								Duration.ofMinutes(5),
								Duration.ofMinutes(2)
						),
						PropagationSignal.CROSS_SERVICE,
						false
				),
				PostExecutionConvergenceRejectionReason
						.STABILIZATION_WINDOW_NOT_SATISFIED
		);
	}

	private PostExecutionVerificationRequirement baseVerificationRequirement() {
		return new PostExecutionVerificationRequirement(
				new ExecutorResponse(
						ExecutorStatus.SUCCESS,
						"exec-1",
						"execution success"
				),
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
														new ReliabilityAssessmentResult(
																RuntimeState.CONVERGED,
																List.of(
																		ReliabilityAssessmentStage
																				.EVIDENCE_CORRELATION
																),
																completeCorrelation(),
																null,
																null,
																null,
																OperationalUncertainty.LOW,
																null
														),
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
				completeCorrelation(),
				true,
				false,
				false
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

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}

	private RegressionSignal signal(RegressionSignalType type, String signalId) {
		return new RegressionSignal(type, signalId, "summary-" + signalId);
	}
}
