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

class OperationalReliabilityExecutionPlanTest {

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
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-19T00:00:00Z");

	private final ExecutionPlanEvaluator evaluator = new ExecutionPlanEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.readOnly()).isTrue();
		assertThat(plan.actionExecution()).isFalse();
		assertThat(plan.actionDispatch()).isFalse();
		assertThat(plan.kubernetesApiCall()).isFalse();
		assertThat(plan.kubectlExecution()).isFalse();
		assertThat(plan.argoCdSync()).isFalse();
		assertThat(plan.terraformApply()).isFalse();
		assertThat(plan.sshOrAnsibleExecution()).isFalse();
		assertThat(plan.executionEngine()).isFalse();
	}

	@Test
	void shouldBeExecutionPlanReadyWhenExecutionPermissionIsReady() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.EXECUTION_PLAN_READY);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.EXECUTION_PERMISSION_READY);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.EXECUTION_PLAN);
	}

	@Test
	void shouldBlockWhenExecutionPlanIdentifierMissing() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				" ",
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.MISSING_EXECUTION_PLAN_IDENTIFIER);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.EXECUTION_PLAN);
	}

	@Test
	void shouldBlockWhenExecutionSequenceMissing() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				" ",
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.MISSING_EXECUTION_SEQUENCE);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.EXECUTION_SEQUENCE);
	}

	@Test
	void shouldBlockWhenRollbackPlanMissing() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				false,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.MISSING_ROLLBACK_PLAN);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.ROLLBACK_PLAN);
	}

	@Test
	void shouldBlockWhenVerificationPlanMissing() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				false,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.MISSING_VERIFICATION_PLAN);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.VERIFICATION_PLAN);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(plan.scope()).isEqualTo(ExecutionPlanScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenExecutionPermissionIsPartial() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionWithStatus(
						ExecutionPermissionIntegrationStatus.PARTIAL_EXECUTION_PERMISSION
				),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.PARTIAL);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.PARTIAL_EXECUTION_PERMISSION);
	}

	@Test
	void shouldRemainNotReadyWhenExecutionPermissionIsNotReady() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionWithStatus(ExecutionPermissionIntegrationStatus.NOT_READY),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.NOT_READY);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.NOT_READY_EXECUTION_PERMISSION);
	}

	@Test
	void shouldRemainUnreliableWhenExecutionPermissionIsUnreliable() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionWithStatus(ExecutionPermissionIntegrationStatus.UNRELIABLE),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.UNRELIABLE);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.UNRELIABLE_EXECUTION_PERMISSION);
	}

	@Test
	void shouldRemainBlockedWhenExecutionPermissionIsBlocked() {
		ExecutionPlan plan = evaluator.evaluate(
				executionPermissionWithStatus(ExecutionPermissionIntegrationStatus.BLOCKED),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(plan.level()).isEqualTo(ExecutionPlanLevel.BLOCKED);
		assertThat(plan.reason()).isEqualTo(ExecutionPlanReason.BLOCKED_EXECUTION_PERMISSION);
	}

	@Test
	void shouldRejectNullExecutionPermissionIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("executionPermissionIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private ExecutionPermissionIntegrationResult executionPermissionReady() {
		return executionPermissionWithStatus(
				ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY
		);
	}

	private ExecutionPermissionIntegrationResult executionPermissionWithStatus(
			ExecutionPermissionIntegrationStatus status
	) {
		return new ExecutionPermissionIntegrationResult(
				executionPermission(status),
				status,
				executionPermissionIntegrationReason(status),
				ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
				status == ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY,
				status == ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY
		);
	}

	private ExecutionPermission executionPermission(
			ExecutionPermissionIntegrationStatus status
	) {
		return new ExecutionPermission(
				executionPermissionLevel(status),
				executionPermissionReason(status),
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

	private ExecutionPermissionLevel executionPermissionLevel(
			ExecutionPermissionIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_PERMISSION_READY -> ExecutionPermissionLevel.EXECUTION_PERMITTED;
			case PARTIAL_EXECUTION_PERMISSION -> ExecutionPermissionLevel.PARTIAL;
			case NOT_READY -> ExecutionPermissionLevel.NOT_READY;
			case UNRELIABLE -> ExecutionPermissionLevel.UNRELIABLE;
			case BLOCKED -> ExecutionPermissionLevel.BLOCKED;
			case UNKNOWN -> ExecutionPermissionLevel.UNKNOWN;
		};
	}

	private ExecutionPermissionReason executionPermissionReason(
			ExecutionPermissionIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_PERMISSION_READY -> ExecutionPermissionReason.ACTION_COMMAND_CANDIDATE_READY;
			case PARTIAL_EXECUTION_PERMISSION -> ExecutionPermissionReason.PARTIAL_ACTION_COMMAND;
			case NOT_READY -> ExecutionPermissionReason.NOT_READY_ACTION_COMMAND;
			case UNRELIABLE -> ExecutionPermissionReason.UNRELIABLE_ACTION_COMMAND;
			case BLOCKED -> ExecutionPermissionReason.BLOCKED_ACTION_COMMAND;
			case UNKNOWN -> ExecutionPermissionReason.UNKNOWN;
		};
	}

	private ExecutionPermissionIntegrationReason executionPermissionIntegrationReason(
			ExecutionPermissionIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_PERMISSION_READY -> ExecutionPermissionIntegrationReason.EXECUTION_PERMITTED;
			case PARTIAL_EXECUTION_PERMISSION -> ExecutionPermissionIntegrationReason.PARTIAL_EXECUTION_PERMISSION;
			case NOT_READY -> ExecutionPermissionIntegrationReason.NOT_READY_EXECUTION_PERMISSION;
			case UNRELIABLE -> ExecutionPermissionIntegrationReason.UNRELIABLE_EXECUTION_PERMISSION;
			case BLOCKED -> ExecutionPermissionIntegrationReason.BLOCKED_EXECUTION_PERMISSION;
			case UNKNOWN -> ExecutionPermissionIntegrationReason.UNKNOWN;
		};
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
