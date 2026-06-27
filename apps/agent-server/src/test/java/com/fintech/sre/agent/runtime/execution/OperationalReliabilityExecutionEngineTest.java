package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.runtime.action.ActionCommand;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationReason;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationResult;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationScope;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationStatus;
import com.fintech.sre.agent.runtime.action.ActionCommandLevel;
import com.fintech.sre.agent.runtime.action.ActionCommandReason;
import com.fintech.sre.agent.runtime.action.ActionCommandScope;
import com.fintech.sre.agent.runtime.approval.ApprovalDecision;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationReason;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationScope;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationStatus;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionLevel;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionReason;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionScope;
import com.fintech.sre.agent.runtime.approval.ApprovalRequest;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationReason;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationScope;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationStatus;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestLevel;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestReason;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestScope;
import com.fintech.sre.agent.runtime.approval.ApprovalState;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationReason;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationScope;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationStatus;
import com.fintech.sre.agent.runtime.approval.ApprovalStateLevel;
import com.fintech.sre.agent.runtime.approval.ApprovalStateReason;
import com.fintech.sre.agent.runtime.approval.ApprovalStateScope;
import com.fintech.sre.agent.runtime.recommendation.RecommendationModelReason;
import com.fintech.sre.agent.runtime.recommendation.RecommendationModelType;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentation;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationReason;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationResult;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationScope;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationStatus;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationReason;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationScope;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationStatus;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.verification.VerificationRequest;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationReason;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationResult;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationScope;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationStatus;
import com.fintech.sre.agent.runtime.verification.VerificationRequestLevel;
import com.fintech.sre.agent.runtime.verification.VerificationRequestReason;
import com.fintech.sre.agent.runtime.verification.VerificationRequestScope;

class OperationalReliabilityExecutionEngineTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String DECISION_IDENTIFIER = "approval-decision/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final String VERIFICATION_REQUEST_IDENTIFIER = "verification-request/payments/001";
	private static final String VERIFICATION_POLICY = "policy/post-change-verification";
	private static final String ACTION_COMMAND_IDENTIFIER = "action-command/payments/001";
	private static final String ACTION_TYPE = "ROLLING_RESTART";
	private static final String TARGET_LAYER = "KUBERNETES_WORKLOAD";
	private static final String BLAST_RADIUS = "namespace/payments-prod";
	private static final String EXECUTION_PERMISSION_IDENTIFIER = "execution-permission/payments/001";
	private static final String OPERATOR_AUTHORIZATION = "authorized/oncall/payments";
	private static final String EXECUTION_PLAN_IDENTIFIER = "execution-plan/payments/001";
	private static final String EXECUTION_SEQUENCE = "step-1: cordon; step-2: rolling restart";
	private static final String DISPATCH_IDENTIFIER = "dispatch/payments/001";
	private static final String EXECUTION_ENDPOINT = "engine://payments-prod/restart";
	private static final String DISPATCH_POLICY = "policy/manual-dispatch";
	private static final String EXECUTION_ENGINE_IDENTIFIER = "execution-engine/payments/001";
	private static final String EXECUTION_ENGINE_TYPE = "KUBERNETES_MANUAL_GATE";
	private static final String EXECUTION_POLICY = "policy/execution-engine-selection";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-20T00:00:00Z");

	private final ExecutionEngineEvaluator evaluator = new ExecutionEngineEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.readOnly()).isTrue();
		assertThat(executionEngine.actionExecution()).isFalse();
		assertThat(executionEngine.actualDispatch()).isFalse();
		assertThat(executionEngine.kubernetesApiCall()).isFalse();
		assertThat(executionEngine.kubectlExecution()).isFalse();
		assertThat(executionEngine.argoCdSync()).isFalse();
		assertThat(executionEngine.terraformApply()).isFalse();
		assertThat(executionEngine.sshOrAnsibleExecution()).isFalse();
		assertThat(executionEngine.specificExecutionEngineImplementation()).isFalse();
	}

	@Test
	void shouldBeExecutionEngineReadyWhenDispatchReadyAndRequirementsPresent() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.EXECUTION_ENGINE_READY);
		assertThat(executionEngine.reason()).isEqualTo(ExecutionEngineReason.DISPATCH_READY);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.EXECUTION_ENGINE);
	}

	@Test
	void shouldBlockWhenExecutionEngineIdentifierMissing() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				" ",
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason())
				.isEqualTo(ExecutionEngineReason.MISSING_EXECUTION_ENGINE_IDENTIFIER);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.EXECUTION_ENGINE);
	}

	@Test
	void shouldBlockWhenExecutionEngineTypeMissing() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				" ",
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason())
				.isEqualTo(ExecutionEngineReason.MISSING_EXECUTION_ENGINE_TYPE);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.EXECUTION_ENGINE_TYPE);
	}

	@Test
	void shouldBlockWhenExecutionEndpointBindingMissing() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				" ",
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason())
				.isEqualTo(ExecutionEngineReason.MISSING_EXECUTION_ENDPOINT_BINDING);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.EXECUTION_ENDPOINT);
	}

	@Test
	void shouldBlockWhenExecutionPolicyMissing() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				" ",
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason())
				.isEqualTo(ExecutionEngineReason.MISSING_EXECUTION_POLICY);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.EXECUTION_POLICY);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason())
				.isEqualTo(ExecutionEngineReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason())
				.isEqualTo(ExecutionEngineReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(executionEngine.scope()).isEqualTo(ExecutionEngineScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenDispatchIsPartial() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.PARTIAL_DISPATCH),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.PARTIAL);
		assertThat(executionEngine.reason()).isEqualTo(ExecutionEngineReason.PARTIAL_DISPATCH);
	}

	@Test
	void shouldRemainNotReadyWhenDispatchIsNotReady() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.NOT_READY),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.NOT_READY);
		assertThat(executionEngine.reason()).isEqualTo(ExecutionEngineReason.NOT_READY_DISPATCH);
	}

	@Test
	void shouldRemainUnreliableWhenDispatchIsUnreliable() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.UNRELIABLE),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.UNRELIABLE);
		assertThat(executionEngine.reason()).isEqualTo(ExecutionEngineReason.UNRELIABLE_DISPATCH);
	}

	@Test
	void shouldRemainBlockedWhenDispatchIsBlocked() {
		ExecutionEngine executionEngine = evaluator.evaluate(
				dispatchIntegrationResult(ExecutionDispatchIntegrationStatus.BLOCKED),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executionEngine.level()).isEqualTo(ExecutionEngineLevel.BLOCKED);
		assertThat(executionEngine.reason()).isEqualTo(ExecutionEngineReason.BLOCKED_DISPATCH);
	}

	@Test
	void shouldRejectNullDispatchIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		)).isInstanceOf(NullPointerException.class)
				.hasMessage("executionDispatchIntegration must not be null");
	}

	private ExecutionDispatchIntegrationResult dispatchIntegrationResult(
			ExecutionDispatchIntegrationStatus status
	) {
		ExecutionDispatchLevel dispatchLevel = switch (status) {
			case DISPATCH_READY_VIEW -> ExecutionDispatchLevel.DISPATCH_READY;
			case PARTIAL_DISPATCH -> ExecutionDispatchLevel.PARTIAL;
			case NOT_READY -> ExecutionDispatchLevel.NOT_READY;
			case UNRELIABLE -> ExecutionDispatchLevel.UNRELIABLE;
			case BLOCKED -> ExecutionDispatchLevel.BLOCKED;
			case UNKNOWN -> ExecutionDispatchLevel.UNKNOWN;
		};
		ExecutionDispatchIntegrationReason reason = switch (status) {
			case DISPATCH_READY_VIEW -> ExecutionDispatchIntegrationReason.DISPATCH_READY;
			case PARTIAL_DISPATCH -> ExecutionDispatchIntegrationReason.PARTIAL_DISPATCH;
			case NOT_READY -> ExecutionDispatchIntegrationReason.NOT_READY_DISPATCH;
			case UNRELIABLE -> ExecutionDispatchIntegrationReason.UNRELIABLE_DISPATCH;
			case BLOCKED -> ExecutionDispatchIntegrationReason.BLOCKED_DISPATCH;
			case UNKNOWN -> ExecutionDispatchIntegrationReason.UNKNOWN;
		};

		return new ExecutionDispatchIntegrationResult(
				new ExecutionDispatch(
						dispatchLevel,
						ExecutionDispatchReason.EXECUTION_PLAN_READY,
						ExecutionDispatchScope.EXECUTION_DISPATCH,
						executionPlanReadyView(),
						DISPATCH_IDENTIFIER,
						EXECUTION_ENDPOINT,
						DISPATCH_POLICY,
						true,
						OperationalUncertainty.LOW,
						false
				),
				status,
				reason,
				ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
				status == ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW,
				status == ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW
		);
	}

	private ExecutionPlanIntegrationResult executionPlanReadyView() {
		return new ExecutionPlanIntegrationResult(
				executionPlan(),
				ExecutionPlanIntegrationStatus.EXECUTION_PLAN_READY_VIEW,
				ExecutionPlanIntegrationReason.EXECUTION_PLAN_READY,
				ExecutionPlanIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionPlan executionPlan() {
		return new ExecutionPlan(
				ExecutionPlanLevel.EXECUTION_PLAN_READY,
				ExecutionPlanReason.EXECUTION_PERMISSION_READY,
				ExecutionPlanScope.EXECUTION_PLAN,
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionPermissionIntegrationResult executionPermissionReady() {
		return new ExecutionPermissionIntegrationResult(
				new ExecutionPermission(
						ExecutionPermissionLevel.EXECUTION_PERMITTED,
						ExecutionPermissionReason.ACTION_COMMAND_CANDIDATE_READY,
						ExecutionPermissionScope.EXECUTION_PERMISSION,
						actionCommandCandidateReady(),
						EXECUTION_PERMISSION_IDENTIFIER,
						"policy/manual-execution-gate",
						OPERATOR_AUTHORIZATION,
						true,
						OperationalUncertainty.LOW,
						false
				),
				ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY,
				ExecutionPermissionIntegrationReason.EXECUTION_PERMITTED,
				ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ActionCommandIntegrationResult actionCommandCandidateReady() {
		return new ActionCommandIntegrationResult(
				actionCommand(),
				ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY,
				ActionCommandIntegrationReason.ACTION_COMMAND_READY,
				ActionCommandIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ActionCommand actionCommand() {
		return new ActionCommand(
				ActionCommandLevel.ACTION_COMMAND_READY,
				ActionCommandReason.VERIFICATION_REQUEST_READY,
				ActionCommandScope.ACTION_COMMAND,
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private VerificationRequestIntegrationResult verificationRequestReady() {
		return new VerificationRequestIntegrationResult(
				verificationRequest(),
				VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY,
				VerificationRequestIntegrationReason.VERIFICATION_REQUESTABLE,
				VerificationRequestIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private VerificationRequest verificationRequest() {
		return new VerificationRequest(
				VerificationRequestLevel.VERIFICATION_REQUESTABLE,
				VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
				VerificationRequestScope.APPROVAL_DECISION,
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalDecisionIntegrationResult approvalDecisionPendingView() {
		return new ApprovalDecisionIntegrationResult(
				approvalDecision(),
				ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW,
				ApprovalDecisionIntegrationReason.DECISION_PENDING,
				ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalDecision approvalDecision() {
		return new ApprovalDecision(
				ApprovalDecisionLevel.DECISION_PENDING,
				ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
				ApprovalDecisionScope.APPROVAL_STATE,
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalStateIntegrationResult approvalPendingView() {
		return new ApprovalStateIntegrationResult(
				approvalState(),
				ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW,
				ApprovalStateIntegrationReason.PENDING_APPROVAL_STATE,
				ApprovalStateIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalState approvalState() {
		return new ApprovalState(
				ApprovalStateLevel.PENDING_APPROVAL,
				ApprovalStateReason.APPROVAL_REQUEST_READY,
				ApprovalStateScope.APPROVAL_REQUEST,
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalRequestIntegrationResult approvalRequestReady() {
		return new ApprovalRequestIntegrationResult(
				approvalRequest(),
				ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY,
				ApprovalRequestIntegrationReason.REQUESTABLE_APPROVAL_REQUEST,
				ApprovalRequestIntegrationScope.APPROVAL_REQUEST,
				true,
				true
		);
	}

	private ApprovalRequest approvalRequest() {
		return new ApprovalRequest(
				ApprovalRequestLevel.REQUESTABLE,
				ApprovalRequestReason.EXPOSABLE_PRESENTATION,
				ApprovalRequestScope.APPROVAL_REQUEST,
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationPresentationIntegrationResult exposablePresentation() {
		return new RecommendationPresentationIntegrationResult(
				presentation(),
				RecommendationPresentationIntegrationStatus.EXPOSABLE,
				RecommendationPresentationIntegrationReason.VALID_RECOMMENDATION_PRESENTATION,
				RecommendationPresentationIntegrationScope.RECOMMENDATION,
				true,
				true
		);
	}

	private RecommendationPresentation presentation() {
		return new RecommendationPresentation(
				"rec-001",
				"Mitigate payment latency degradation",
				"Use the matched runbook with rollback and verification references.",
				RecommendationModelType.INCIDENT_RESPONSE,
				RecommendationModelReason.SCENARIO_MATCH,
				"scenario/payments-degradation",
				"runbook/payment-latency-mitigation",
				"rollback/payments",
				"verification/payments",
				"evidence/payment-latency-correlation",
				"PAYMENT_SAFE_REVIEWED",
				PRESENTED_AT,
				RecommendationPresentationStatus.PRESENTABLE,
				RecommendationPresentationReason.VALID_RECOMMENDATION,
				RecommendationPresentationScope.PRESENTATION
		);
	}
}
