package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutorPortSkeletonTest {

	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier riskClassifier =
			new ReliabilityRiskClassifier();
	private final HumanApprovalPolicy humanApprovalPolicy =
			new HumanApprovalPolicy();
	private final ReliabilityRecommendationBoundary recommendationBoundary =
			new ReliabilityRecommendationBoundary();
	private final ActionCommandBoundary actionCommandBoundary =
			new ActionCommandBoundary();
	private final ScenarioBinding scenarioBinding = new ScenarioBinding();
	private final RollbackVerificationBinding rollbackVerificationBinding =
			new RollbackVerificationBinding();
	private final SafetyPolicyGate safetyPolicyGate = new SafetyPolicyGate();
	private final ActionAdmissionGate actionAdmissionGate = new ActionAdmissionGate();
	private final ExecutionBoundary executionBoundary = new ExecutionBoundary();
	private final ReliabilityExecutorContract executorContract =
			new ReliabilityExecutorContract();
	private final ExecutionReadinessGate executionReadinessGate =
			new ExecutionReadinessGate();

	@Test
	void shouldRejectExecutorRequestCreationWithoutAcceptedReadiness() {
		assertThatThrownBy(() -> new ExecutorRequest(
				rejectedReadinessDecision(),
				"audit-trace-1"
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("executor request requires accepted execution readiness");
	}

	@Test
	void shouldRemainInterfaceContractOnly() {
		assertThat(ReliabilityExecutorPort.class.isInterface()).isTrue();
	}

	@Test
	void shouldNotTreatPortAsActualExecutorImplementation() {
		ExecutorRequest request = new ExecutorRequest(
				acceptedReadinessDecision(false),
				null
		);

		assertThat(request.readinessDecision().semanticGateOnly()).isTrue();
	}

	@Test
	void shouldExposeSuccessFailureUnknownStatuses() {
		assertThat(ExecutorStatus.values()).containsExactly(
				ExecutorStatus.SUCCESS,
				ExecutorStatus.FAILURE,
				ExecutorStatus.UNKNOWN
		);
	}

	@Test
	void shouldNotTreatExecutorSuccessAsConvergence() {
		ExecutorResponse response = new ExecutorResponse(
				ExecutorStatus.SUCCESS,
				"exec-1",
				"execution success"
		);

		assertThat(response.converged()).isFalse();
	}

	@Test
	void shouldRequireVerificationOrRollbackPathAfterExecutorFailure() {
		ExecutorResponse response = new ExecutorResponse(
				ExecutorStatus.FAILURE,
				"exec-1",
				"execution failed"
		);

		assertThat(response.requiresVerificationOrRollbackPath()).isTrue();
	}

	@Test
	void shouldRequireAuditTraceForPaymentImpactingExecution() {
		assertThatThrownBy(() -> new ExecutorRequest(
				acceptedReadinessDecision(true),
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("payment-impacting executor request requires audit trace id");
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		ExecutorRequest request = new ExecutorRequest(
				acceptedReadinessDecision(false),
				null
		);

		assertThat(request.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullReadinessDecision() {
		assertThatThrownBy(() -> new ExecutorRequest(null, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("readinessDecision must not be null");
	}

	private ExecutionReadinessDecision rejectedReadinessDecision() {
		return new ExecutionReadinessDecision(
				false,
				ExecutionReadinessScope.NONE,
				readinessRequirement(false),
				ExecutionReadinessRejectionReason.EXECUTION_BOUNDARY_REJECTED
		);
	}

	private ExecutionReadinessDecision acceptedReadinessDecision(
			boolean paymentImpacting
	) {
		return new ExecutionReadinessDecision(
				true,
				ExecutionReadinessScope.READY,
				readinessRequirement(paymentImpacting),
				null
		);
	}

	private ExecutionReadinessRequirement readinessRequirement(
			boolean paymentImpacting
	) {
		ExecutionBoundaryDecision boundaryDecision = eligibleExecutionDecision(
				paymentImpacting
		);
		ExecutionPlan plan = executorContract.plan(new ExecutionIntent(
				boundaryDecision,
				"rollback-plan-1",
				"verification-plan-1",
				paymentImpacting,
				paymentImpacting,
				false,
				true
		));
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(ExecutionAuditEventType.APPROVAL_RECORDED, "approval-1"))
				.append(event(ExecutionAuditEventType.ELIGIBILITY_RECORDED, "eligibility-1"))
				.append(event(ExecutionAuditEventType.PLAN_CREATED, "plan-1"));
		return new ExecutionReadinessRequirement(
				boundaryDecision,
				plan,
				trail.verify(paymentImpacting)
		);
	}

	private ExecutionBoundaryDecision eligibleExecutionDecision(
			boolean paymentImpacting
	) {
		ActionAdmissionDecision actionAdmissionDecision = actionAdmissionDecision(
				paymentImpacting
		);
		return executionBoundary.evaluate(new ExecutionRequirement(
				actionAdmissionDecision,
				true,
				true,
				true,
				true
		));
	}

	private ActionAdmissionDecision actionAdmissionDecision(
			boolean paymentImpacting
	) {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false
		);
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED)
		);
		RecommendationEligibility recommendationEligibility =
				new RecommendationEligibility(
						true,
						RecommendationScope.ADVISORY_WITH_ROLLBACK_AND_VERIFICATION_REQUIREMENT,
						true,
						true,
						List.of(RecommendationRestriction.EXECUTION_AUTHORITY_PROHIBITED),
						List.of(
								RecommendationBoundaryReason
										.ASSESSMENT_RESULT_IS_NOT_RECOMMENDATION,
								RecommendationBoundaryReason
										.AI_RECOMMENDATION_IS_ADVISORY_ONLY
						)
				);
		ActionCommandEligibility actionCommandEligibility = new ActionCommandEligibility(
				true,
				new ActionCommandRequirement(true, true, true),
				List.of(
						ActionCommandRestriction.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
						ActionCommandRestriction
								.RECOMMENDATION_ELIGIBILITY_IS_NOT_ACTION_ELIGIBILITY
				),
				List.of(
						ActionCommandBoundaryReason.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
						ActionCommandBoundaryReason
								.RECOMMENDATION_ELIGIBILITY_DOES_NOT_GRANT_ACTION_ELIGIBILITY
				)
		);
		ScenarioBindingDecision scenarioBindingDecision = scenarioBinding.bind(
				new ScenarioReference("scenario-known", "portfolio-runtime", true, false)
		);
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				rollbackVerificationBinding.bind(
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
								paymentImpacting
						),
						paymentImpacting
				);
		SafetyPolicyDecision safetyPolicyDecision = safetyPolicyGate.evaluate(
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						true,
						true,
						paymentImpacting
				)
		);
		return actionAdmissionGate.evaluate(new ActionAdmissionRequirement(
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility,
				actionCommandEligibility,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				safetyPolicyDecision,
				false
		));
	}

	private ReliabilityAssessmentResult assess(
			RuntimeState runtimeState,
			List<EvidenceSignal> evidenceSignals,
			boolean contradictoryEvidence,
			boolean propagationActive
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
				List.of()
		));
	}

	private ExecutionAuditEvent event(ExecutionAuditEventType type, String id) {
		return new ExecutionAuditEvent(
				type,
				id,
				"summary-" + id,
				Instant.parse("2026-05-28T00:00:00Z")
		);
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
