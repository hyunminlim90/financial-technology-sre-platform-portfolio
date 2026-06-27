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

class OperationalReliabilityExecutionPlanIntegrationTest {

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
	private static final String EXECUTION_POLICY = "policy/manual-execution-gate";
	private static final String OPERATOR_AUTHORIZATION = "authorized/oncall/payments";
	private static final String EXECUTION_PLAN_IDENTIFIER = "execution-plan/payments/001";
	private static final String EXECUTION_SEQUENCE = "step-1: cordon; step-2: rolling restart";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-20T00:00:00Z");

	private final ExecutionPlanIntegration integration = new ExecutionPlanIntegration();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				executionPlanWithLevel(ExecutionPlanLevel.EXECUTION_PLAN_READY)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.actionExecution()).isFalse();
		assertThat(result.actionDispatch()).isFalse();
		assertThat(result.kubernetesApiCall()).isFalse();
		assertThat(result.kubectlExecution()).isFalse();
		assertThat(result.argoCdSync()).isFalse();
		assertThat(result.terraformApply()).isFalse();
		assertThat(result.sshOrAnsibleExecution()).isFalse();
		assertThat(result.executionEngine()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeExecutionPlanReadyViewWhenExecutionPlanIsReady() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				executionPlanWithLevel(ExecutionPlanLevel.EXECUTION_PLAN_READY)
		);

		assertThat(result.status())
				.isEqualTo(ExecutionPlanIntegrationStatus.EXECUTION_PLAN_READY_VIEW);
		assertThat(result.reason())
				.isEqualTo(ExecutionPlanIntegrationReason.EXECUTION_PLAN_READY);
		assertThat(result.scope())
				.isEqualTo(ExecutionPlanIntegrationScope.OPERATOR_VIEW);
		assertThat(result.operatorFacingExecutionPlanVisible()).isTrue();
		assertThat(result.executionPlanCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRemainPartialWhenExecutionPlanIsPartial() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				executionPlanWithLevel(ExecutionPlanLevel.PARTIAL)
		);

		assertThat(result.status())
				.isEqualTo(ExecutionPlanIntegrationStatus.PARTIAL_EXECUTION_PLAN);
		assertThat(result.reason())
				.isEqualTo(ExecutionPlanIntegrationReason.PARTIAL_EXECUTION_PLAN);
	}

	@Test
	void shouldRemainNotReadyWhenExecutionPlanIsNotReady() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				executionPlanWithLevel(ExecutionPlanLevel.NOT_READY)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.NOT_READY);
		assertThat(result.reason())
				.isEqualTo(ExecutionPlanIntegrationReason.NOT_READY_EXECUTION_PLAN);
	}

	@Test
	void shouldRemainUnreliableWhenExecutionPlanIsUnreliable() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				executionPlanWithLevel(ExecutionPlanLevel.UNRELIABLE)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.UNRELIABLE);
		assertThat(result.reason())
				.isEqualTo(ExecutionPlanIntegrationReason.UNRELIABLE_EXECUTION_PLAN);
	}

	@Test
	void shouldRemainBlockedWhenExecutionPlanIsBlocked() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				executionPlanWithLevel(ExecutionPlanLevel.BLOCKED)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(ExecutionPlanIntegrationReason.BLOCKED_EXECUTION_PLAN);
	}

	@Test
	void shouldBlockWhenExecutionPlanIdentifierMissing() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				new ExecutionPlan(
						ExecutionPlanLevel.EXECUTION_PLAN_READY,
						ExecutionPlanReason.EXECUTION_PERMISSION_READY,
						ExecutionPlanScope.EXECUTION_PLAN,
						executionPermissionReady(),
						" ",
						EXECUTION_SEQUENCE,
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ExecutionPlanIntegrationReason.MISSING_EXECUTION_PLAN_IDENTIFIER
		);
		assertThat(result.scope()).isEqualTo(ExecutionPlanIntegrationScope.EXECUTION_PLAN);
	}

	@Test
	void shouldBlockWhenExecutionSequenceMissing() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				new ExecutionPlan(
						ExecutionPlanLevel.EXECUTION_PLAN_READY,
						ExecutionPlanReason.EXECUTION_PERMISSION_READY,
						ExecutionPlanScope.EXECUTION_PLAN,
						executionPermissionReady(),
						EXECUTION_PLAN_IDENTIFIER,
						" ",
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ExecutionPlanIntegrationReason.MISSING_EXECUTION_SEQUENCE
		);
		assertThat(result.scope()).isEqualTo(ExecutionPlanIntegrationScope.EXECUTION_SEQUENCE);
	}

	@Test
	void shouldBlockWhenRollbackPlanMissing() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				new ExecutionPlan(
						ExecutionPlanLevel.EXECUTION_PLAN_READY,
						ExecutionPlanReason.EXECUTION_PERMISSION_READY,
						ExecutionPlanScope.EXECUTION_PLAN,
						executionPermissionReady(),
						EXECUTION_PLAN_IDENTIFIER,
						EXECUTION_SEQUENCE,
						false,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ExecutionPlanIntegrationReason.MISSING_ROLLBACK_PLAN
		);
		assertThat(result.scope()).isEqualTo(ExecutionPlanIntegrationScope.ROLLBACK_PLAN);
	}

	@Test
	void shouldBlockWhenVerificationPlanMissing() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				new ExecutionPlan(
						ExecutionPlanLevel.EXECUTION_PLAN_READY,
						ExecutionPlanReason.EXECUTION_PERMISSION_READY,
						ExecutionPlanScope.EXECUTION_PLAN,
						executionPermissionReady(),
						EXECUTION_PLAN_IDENTIFIER,
						EXECUTION_SEQUENCE,
						true,
						false,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ExecutionPlanIntegrationReason.MISSING_VERIFICATION_PLAN
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionPlanIntegrationScope.VERIFICATION_PLAN
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				new ExecutionPlan(
						ExecutionPlanLevel.EXECUTION_PLAN_READY,
						ExecutionPlanReason.EXECUTION_PERMISSION_READY,
						ExecutionPlanScope.EXECUTION_PLAN,
						executionPermissionReady(),
						EXECUTION_PLAN_IDENTIFIER,
						EXECUTION_SEQUENCE,
						true,
						true,
						OperationalUncertainty.LOW,
						true
				)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ExecutionPlanIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(ExecutionPlanIntegrationScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionPlanIntegrationResult result = integration.integrate(
				new ExecutionPlan(
						ExecutionPlanLevel.EXECUTION_PLAN_READY,
						ExecutionPlanReason.EXECUTION_PERMISSION_READY,
						ExecutionPlanScope.EXECUTION_PLAN,
						executionPermissionReady(),
						EXECUTION_PLAN_IDENTIFIER,
						EXECUTION_SEQUENCE,
						true,
						true,
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ExecutionPlanIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ExecutionPlanIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(ExecutionPlanIntegrationScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullExecutionPlan() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("executionPlan must not be null");
	}

	private ExecutionPlan executionPlanWithLevel(ExecutionPlanLevel level) {
		return new ExecutionPlan(
				level,
				executionPlanReason(level),
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

	private ExecutionPlanReason executionPlanReason(ExecutionPlanLevel level) {
		return switch (level) {
			case EXECUTION_PLAN_READY -> ExecutionPlanReason.EXECUTION_PERMISSION_READY;
			case PARTIAL -> ExecutionPlanReason.PARTIAL_EXECUTION_PERMISSION;
			case NOT_READY -> ExecutionPlanReason.NOT_READY_EXECUTION_PERMISSION;
			case UNRELIABLE -> ExecutionPlanReason.UNRELIABLE_EXECUTION_PERMISSION;
			case BLOCKED -> ExecutionPlanReason.BLOCKED_EXECUTION_PERMISSION;
			case UNKNOWN -> ExecutionPlanReason.UNKNOWN;
		};
	}

	private ExecutionPermissionIntegrationResult executionPermissionReady() {
		return new ExecutionPermissionIntegrationResult(
				executionPermission(),
				ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY,
				ExecutionPermissionIntegrationReason.EXECUTION_PERMITTED,
				ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionPermission executionPermission() {
		return new ExecutionPermission(
				ExecutionPermissionLevel.EXECUTION_PERMITTED,
				ExecutionPermissionReason.ACTION_COMMAND_CANDIDATE_READY,
				ExecutionPermissionScope.EXECUTION_PERMISSION,
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
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
