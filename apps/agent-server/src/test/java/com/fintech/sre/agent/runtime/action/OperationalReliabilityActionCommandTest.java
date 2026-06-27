package com.fintech.sre.agent.runtime.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

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

class OperationalReliabilityActionCommandTest {

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
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-18T00:00:00Z");

	private final ActionCommandEvaluator evaluator = new ActionCommandEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ActionCommand command = evaluator.evaluate(
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

		assertThat(command.readOnly()).isTrue();
		assertThat(command.actionExecution()).isFalse();
		assertThat(command.actionDispatch()).isFalse();
		assertThat(command.kubernetesApiCall()).isFalse();
		assertThat(command.argoCdSync()).isFalse();
		assertThat(command.terraformApply()).isFalse();
		assertThat(command.executionPermission()).isFalse();
	}

	@Test
	void shouldBeActionCommandReadyWhenVerificationRequestIsReady() {
		ActionCommand command = evaluator.evaluate(
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

		assertThat(command.level()).isEqualTo(ActionCommandLevel.ACTION_COMMAND_READY);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.VERIFICATION_REQUEST_READY);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.ACTION_COMMAND);
	}

	@Test
	void shouldBlockWhenActionCommandIdentifierMissing() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				" ",
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.MISSING_ACTION_COMMAND_IDENTIFIER);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.ACTION_COMMAND);
	}

	@Test
	void shouldBlockWhenActionTypeMissing() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				" ",
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.MISSING_ACTION_TYPE);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.ACTION_TYPE);
	}

	@Test
	void shouldBlockWhenTargetLayerMissing() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				" ",
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.MISSING_TARGET_LAYER);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.TARGET_LAYER);
	}

	@Test
	void shouldBlockWhenBlastRadiusBoundaryMissing() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				" ",
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.MISSING_BLAST_RADIUS_BOUNDARY);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.BLAST_RADIUS);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				false,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.MISSING_ROLLBACK_BINDING);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.ROLLBACK);
	}

	@Test
	void shouldBlockWhenVerificationBindingMissing() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				false,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.MISSING_VERIFICATION_BINDING);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.VERIFICATION);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(command.scope()).isEqualTo(ActionCommandScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenVerificationRequestIsPartial() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestWithStatus(VerificationRequestIntegrationStatus.PARTIAL_VERIFICATION_REQUEST),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.PARTIAL);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.PARTIAL_VERIFICATION_REQUEST);
	}

	@Test
	void shouldRemainNotReadyWhenVerificationRequestIsNotReady() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestWithStatus(VerificationRequestIntegrationStatus.NOT_READY),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.NOT_READY);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.NOT_READY_VERIFICATION_REQUEST);
	}

	@Test
	void shouldRemainUnreliableWhenVerificationRequestIsUnreliable() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestWithStatus(VerificationRequestIntegrationStatus.UNRELIABLE),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.UNRELIABLE);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.UNRELIABLE_VERIFICATION_REQUEST);
	}

	@Test
	void shouldRemainBlockedWhenVerificationRequestIsBlocked() {
		ActionCommand command = evaluator.evaluate(
				verificationRequestWithStatus(VerificationRequestIntegrationStatus.BLOCKED),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(command.level()).isEqualTo(ActionCommandLevel.BLOCKED);
		assertThat(command.reason()).isEqualTo(ActionCommandReason.BLOCKED_VERIFICATION_REQUEST);
	}

	@Test
	void shouldRejectNullVerificationRequestIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("verificationRequestIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private VerificationRequestIntegrationResult verificationRequestReady() {
		return verificationRequestWithStatus(VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY);
	}

	private VerificationRequestIntegrationResult verificationRequestWithStatus(
			VerificationRequestIntegrationStatus status
	) {
		return new VerificationRequestIntegrationResult(
				verificationRequest(status),
				status,
				verificationIntegrationReason(status),
				VerificationRequestIntegrationScope.OPERATOR_VIEW,
				status == VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY,
				status == VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY
		);
	}

	private VerificationRequest verificationRequest(VerificationRequestIntegrationStatus status) {
		return new VerificationRequest(
				verificationRequestLevel(status),
				verificationRequestReason(status),
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

	private VerificationRequestLevel verificationRequestLevel(
			VerificationRequestIntegrationStatus status
	) {
		return switch (status) {
			case VERIFICATION_REQUEST_READY -> VerificationRequestLevel.VERIFICATION_REQUESTABLE;
			case PARTIAL_VERIFICATION_REQUEST -> VerificationRequestLevel.PARTIAL;
			case NOT_READY -> VerificationRequestLevel.NOT_READY;
			case UNRELIABLE -> VerificationRequestLevel.UNRELIABLE;
			case BLOCKED -> VerificationRequestLevel.BLOCKED;
			case UNKNOWN -> VerificationRequestLevel.UNKNOWN;
		};
	}

	private VerificationRequestReason verificationRequestReason(
			VerificationRequestIntegrationStatus status
	) {
		return switch (status) {
			case VERIFICATION_REQUEST_READY -> VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW;
			case PARTIAL_VERIFICATION_REQUEST -> VerificationRequestReason.PARTIAL_APPROVAL_DECISION;
			case NOT_READY -> VerificationRequestReason.NOT_READY_APPROVAL_DECISION;
			case UNRELIABLE -> VerificationRequestReason.UNRELIABLE_APPROVAL_DECISION;
			case BLOCKED -> VerificationRequestReason.BLOCKED_APPROVAL_DECISION;
			case UNKNOWN -> VerificationRequestReason.UNKNOWN;
		};
	}

	private VerificationRequestIntegrationReason verificationIntegrationReason(
			VerificationRequestIntegrationStatus status
	) {
		return switch (status) {
			case VERIFICATION_REQUEST_READY -> VerificationRequestIntegrationReason.VERIFICATION_REQUESTABLE;
			case PARTIAL_VERIFICATION_REQUEST -> VerificationRequestIntegrationReason.PARTIAL_VERIFICATION_REQUEST;
			case NOT_READY -> VerificationRequestIntegrationReason.NOT_READY_VERIFICATION_REQUEST;
			case UNRELIABLE -> VerificationRequestIntegrationReason.UNRELIABLE_VERIFICATION_REQUEST;
			case BLOCKED -> VerificationRequestIntegrationReason.BLOCKED_VERIFICATION_REQUEST;
			case UNKNOWN -> VerificationRequestIntegrationReason.UNKNOWN;
		};
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
