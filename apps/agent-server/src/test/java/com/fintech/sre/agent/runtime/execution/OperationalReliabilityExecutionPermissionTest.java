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

class OperationalReliabilityExecutionPermissionTest {

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
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-19T00:00:00Z");

	private final ExecutionPermissionEvaluator evaluator =
			new ExecutionPermissionEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.readOnly()).isTrue();
		assertThat(permission.actionExecution()).isFalse();
		assertThat(permission.actionDispatch()).isFalse();
		assertThat(permission.kubernetesApiCall()).isFalse();
		assertThat(permission.kubectlExecution()).isFalse();
		assertThat(permission.argoCdSync()).isFalse();
		assertThat(permission.terraformApply()).isFalse();
		assertThat(permission.sshOrAnsibleExecution()).isFalse();
	}

	@Test
	void shouldBeExecutionPermittedWhenActionCommandCandidateReady() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.EXECUTION_PERMITTED);
		assertThat(permission.reason()).isEqualTo(ExecutionPermissionReason.ACTION_COMMAND_CANDIDATE_READY);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.EXECUTION_PERMISSION);
	}

	@Test
	void shouldBlockWhenExecutionPermissionIdentifierMissing() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				" ",
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.MISSING_EXECUTION_PERMISSION_IDENTIFIER
		);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.EXECUTION_PERMISSION);
	}

	@Test
	void shouldBlockWhenExecutionPolicyMissing() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				" ",
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.MISSING_EXECUTION_POLICY
		);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.EXECUTION_POLICY);
	}

	@Test
	void shouldBlockWhenOperatorAuthorizationMissing() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				" ",
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.MISSING_OPERATOR_AUTHORIZATION
		);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.OPERATOR_AUTHORIZATION);
	}

	@Test
	void shouldBlockWhenExecutionGuardrailMissing() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				false,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.MISSING_EXECUTION_GUARDRAIL
		);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.EXECUTION_GUARDRAIL);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(permission.scope()).isEqualTo(ExecutionPermissionScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenActionCommandIsPartial() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandWithStatus(ActionCommandIntegrationStatus.PARTIAL_ACTION_COMMAND),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.PARTIAL);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.PARTIAL_ACTION_COMMAND
		);
	}

	@Test
	void shouldRemainNotReadyWhenActionCommandIsNotReady() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandWithStatus(ActionCommandIntegrationStatus.NOT_READY),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.NOT_READY);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.NOT_READY_ACTION_COMMAND
		);
	}

	@Test
	void shouldRemainUnreliableWhenActionCommandIsUnreliable() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandWithStatus(ActionCommandIntegrationStatus.UNRELIABLE),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.UNRELIABLE);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.UNRELIABLE_ACTION_COMMAND
		);
	}

	@Test
	void shouldRemainBlockedWhenActionCommandIsBlocked() {
		ExecutionPermission permission = evaluator.evaluate(
				actionCommandWithStatus(ActionCommandIntegrationStatus.BLOCKED),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(permission.level()).isEqualTo(ExecutionPermissionLevel.BLOCKED);
		assertThat(permission.reason()).isEqualTo(
				ExecutionPermissionReason.BLOCKED_ACTION_COMMAND
		);
	}

	@Test
	void shouldRejectNullActionCommandIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("actionCommandIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				actionCommandCandidateReady(),
				EXECUTION_PERMISSION_IDENTIFIER,
				EXECUTION_POLICY,
				OPERATOR_AUTHORIZATION,
				true,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private ActionCommandIntegrationResult actionCommandCandidateReady() {
		return actionCommandWithStatus(
				ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY
		);
	}

	private ActionCommandIntegrationResult actionCommandWithStatus(
			ActionCommandIntegrationStatus status
	) {
		return new ActionCommandIntegrationResult(
				actionCommand(status),
				status,
				actionCommandIntegrationReason(status),
				ActionCommandIntegrationScope.OPERATOR_VIEW,
				status == ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY,
				status == ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY
		);
	}

	private ActionCommand actionCommand(ActionCommandIntegrationStatus status) {
		return new ActionCommand(
				actionCommandLevel(status),
				actionCommandReason(status),
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

	private ActionCommandLevel actionCommandLevel(ActionCommandIntegrationStatus status) {
		return switch (status) {
			case ACTION_COMMAND_CANDIDATE_READY -> ActionCommandLevel.ACTION_COMMAND_READY;
			case PARTIAL_ACTION_COMMAND -> ActionCommandLevel.PARTIAL;
			case NOT_READY -> ActionCommandLevel.NOT_READY;
			case UNRELIABLE -> ActionCommandLevel.UNRELIABLE;
			case BLOCKED -> ActionCommandLevel.BLOCKED;
			case UNKNOWN -> ActionCommandLevel.UNKNOWN;
		};
	}

	private ActionCommandReason actionCommandReason(ActionCommandIntegrationStatus status) {
		return switch (status) {
			case ACTION_COMMAND_CANDIDATE_READY -> ActionCommandReason.VERIFICATION_REQUEST_READY;
			case PARTIAL_ACTION_COMMAND -> ActionCommandReason.PARTIAL_VERIFICATION_REQUEST;
			case NOT_READY -> ActionCommandReason.NOT_READY_VERIFICATION_REQUEST;
			case UNRELIABLE -> ActionCommandReason.UNRELIABLE_VERIFICATION_REQUEST;
			case BLOCKED -> ActionCommandReason.BLOCKED_VERIFICATION_REQUEST;
			case UNKNOWN -> ActionCommandReason.UNKNOWN;
		};
	}

	private ActionCommandIntegrationReason actionCommandIntegrationReason(
			ActionCommandIntegrationStatus status
	) {
		return switch (status) {
			case ACTION_COMMAND_CANDIDATE_READY -> ActionCommandIntegrationReason.ACTION_COMMAND_READY;
			case PARTIAL_ACTION_COMMAND -> ActionCommandIntegrationReason.PARTIAL_ACTION_COMMAND;
			case NOT_READY -> ActionCommandIntegrationReason.NOT_READY_ACTION_COMMAND;
			case UNRELIABLE -> ActionCommandIntegrationReason.UNRELIABLE_ACTION_COMMAND;
			case BLOCKED -> ActionCommandIntegrationReason.BLOCKED_ACTION_COMMAND;
			case UNKNOWN -> ActionCommandIntegrationReason.UNKNOWN;
		};
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
