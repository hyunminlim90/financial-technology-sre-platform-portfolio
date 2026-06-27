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

class OperationalReliabilityActionCommandIntegrationTest {

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
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-19T00:00:00Z");

	private final ActionCommandIntegration integration = new ActionCommandIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ActionCommandIntegrationResult result = integration.integrate(
				actionCommandWithLevel(ActionCommandLevel.ACTION_COMMAND_READY)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.actionExecution()).isFalse();
		assertThat(result.actionDispatch()).isFalse();
		assertThat(result.kubernetesApiCall()).isFalse();
		assertThat(result.argoCdSync()).isFalse();
		assertThat(result.terraformApply()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeActionCommandCandidateReadyWhenCommandIsReady() {
		ActionCommandIntegrationResult result = integration.integrate(
				actionCommandWithLevel(ActionCommandLevel.ACTION_COMMAND_READY)
		);

		assertThat(result.status())
				.isEqualTo(ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY);
		assertThat(result.reason())
				.isEqualTo(ActionCommandIntegrationReason.ACTION_COMMAND_READY);
		assertThat(result.scope())
				.isEqualTo(ActionCommandIntegrationScope.OPERATOR_VIEW);
		assertThat(result.operatorFacingActionCommandCandidateVisible()).isTrue();
		assertThat(result.actionCommandCandidateCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRemainPartialWhenCommandIsPartial() {
		ActionCommandIntegrationResult result = integration.integrate(
				actionCommandWithLevel(ActionCommandLevel.PARTIAL)
		);

		assertThat(result.status())
				.isEqualTo(ActionCommandIntegrationStatus.PARTIAL_ACTION_COMMAND);
		assertThat(result.reason())
				.isEqualTo(ActionCommandIntegrationReason.PARTIAL_ACTION_COMMAND);
	}

	@Test
	void shouldRemainNotReadyWhenCommandIsNotReady() {
		ActionCommandIntegrationResult result = integration.integrate(
				actionCommandWithLevel(ActionCommandLevel.NOT_READY)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.NOT_READY);
		assertThat(result.reason())
				.isEqualTo(ActionCommandIntegrationReason.NOT_READY_ACTION_COMMAND);
	}

	@Test
	void shouldRemainUnreliableWhenCommandIsUnreliable() {
		ActionCommandIntegrationResult result = integration.integrate(
				actionCommandWithLevel(ActionCommandLevel.UNRELIABLE)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.UNRELIABLE);
		assertThat(result.reason())
				.isEqualTo(ActionCommandIntegrationReason.UNRELIABLE_ACTION_COMMAND);
	}

	@Test
	void shouldRemainBlockedWhenCommandIsBlocked() {
		ActionCommandIntegrationResult result = integration.integrate(
				actionCommandWithLevel(ActionCommandLevel.BLOCKED)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(ActionCommandIntegrationReason.BLOCKED_ACTION_COMMAND);
	}

	@Test
	void shouldBlockWhenActionCommandIdentifierMissing() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
						ActionCommandLevel.ACTION_COMMAND_READY,
						ActionCommandReason.VERIFICATION_REQUEST_READY,
						ActionCommandScope.ACTION_COMMAND,
						verificationRequestReady(),
						" ",
						ACTION_TYPE,
						TARGET_LAYER,
						BLAST_RADIUS,
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.MISSING_ACTION_COMMAND_IDENTIFIER
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.ACTION_COMMAND);
	}

	@Test
	void shouldBlockWhenActionTypeMissing() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
						ActionCommandLevel.ACTION_COMMAND_READY,
						ActionCommandReason.VERIFICATION_REQUEST_READY,
						ActionCommandScope.ACTION_COMMAND,
						verificationRequestReady(),
						ACTION_COMMAND_IDENTIFIER,
						" ",
						TARGET_LAYER,
						BLAST_RADIUS,
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.MISSING_ACTION_TYPE
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.ACTION_TYPE);
	}

	@Test
	void shouldBlockWhenTargetLayerMissing() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
						ActionCommandLevel.ACTION_COMMAND_READY,
						ActionCommandReason.VERIFICATION_REQUEST_READY,
						ActionCommandScope.ACTION_COMMAND,
						verificationRequestReady(),
						ACTION_COMMAND_IDENTIFIER,
						ACTION_TYPE,
						" ",
						BLAST_RADIUS,
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.MISSING_TARGET_LAYER
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.TARGET_LAYER);
	}

	@Test
	void shouldBlockWhenBlastRadiusBoundaryMissing() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
						ActionCommandLevel.ACTION_COMMAND_READY,
						ActionCommandReason.VERIFICATION_REQUEST_READY,
						ActionCommandScope.ACTION_COMMAND,
						verificationRequestReady(),
						ACTION_COMMAND_IDENTIFIER,
						ACTION_TYPE,
						TARGET_LAYER,
						" ",
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.MISSING_BLAST_RADIUS_BOUNDARY
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.BLAST_RADIUS);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
						ActionCommandLevel.ACTION_COMMAND_READY,
						ActionCommandReason.VERIFICATION_REQUEST_READY,
						ActionCommandScope.ACTION_COMMAND,
						verificationRequestReady(),
						ACTION_COMMAND_IDENTIFIER,
						ACTION_TYPE,
						TARGET_LAYER,
						BLAST_RADIUS,
						false,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.MISSING_ROLLBACK_BINDING
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.ROLLBACK);
	}

	@Test
	void shouldBlockWhenVerificationBindingMissing() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
						ActionCommandLevel.ACTION_COMMAND_READY,
						ActionCommandReason.VERIFICATION_REQUEST_READY,
						ActionCommandScope.ACTION_COMMAND,
						verificationRequestReady(),
						ACTION_COMMAND_IDENTIFIER,
						ACTION_TYPE,
						TARGET_LAYER,
						BLAST_RADIUS,
						true,
						false,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.MISSING_VERIFICATION_BINDING
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.VERIFICATION);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
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
						true
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ActionCommandIntegrationResult result = integration.integrate(
				new ActionCommand(
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
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ActionCommandIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ActionCommandIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(ActionCommandIntegrationScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullActionCommand() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("actionCommand must not be null");
	}

	private ActionCommand actionCommandWithLevel(ActionCommandLevel level) {
		return new ActionCommand(
				level,
				actionCommandReason(level),
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

	private ActionCommandReason actionCommandReason(ActionCommandLevel level) {
		return switch (level) {
			case ACTION_COMMAND_READY -> ActionCommandReason.VERIFICATION_REQUEST_READY;
			case PARTIAL -> ActionCommandReason.PARTIAL_VERIFICATION_REQUEST;
			case NOT_READY -> ActionCommandReason.NOT_READY_VERIFICATION_REQUEST;
			case UNRELIABLE -> ActionCommandReason.UNRELIABLE_VERIFICATION_REQUEST;
			case BLOCKED -> ActionCommandReason.BLOCKED_VERIFICATION_REQUEST;
			case UNKNOWN -> ActionCommandReason.UNKNOWN;
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
