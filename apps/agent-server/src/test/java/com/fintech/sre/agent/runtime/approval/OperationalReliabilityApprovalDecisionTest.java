package com.fintech.sre.agent.runtime.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

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

class OperationalReliabilityApprovalDecisionTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String DECISION_IDENTIFIER = "approval-decision/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-15T00:00:00Z");

	private final ApprovalDecisionEvaluator evaluator =
			new ApprovalDecisionEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.readOnly()).isTrue();
		assertThat(decision.humanApprovalPerformed()).isFalse();
		assertThat(decision.approvalResult()).isFalse();
		assertThat(decision.verificationRequest()).isFalse();
		assertThat(decision.actionCommand()).isFalse();
		assertThat(decision.executionPermission()).isFalse();
	}

	@Test
	void shouldBeDecisionPendingWhenApprovalPendingViewExists() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.DECISION_PENDING);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.APPROVAL_PENDING_VIEW);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.APPROVAL_STATE);
	}

	@Test
	void shouldBlockWhenDecisionIdentifierMissing() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				" ",
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.MISSING_DECISION_IDENTIFIER);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.APPROVAL_DECISION);
	}

	@Test
	void shouldBlockWhenApprovalPolicyMissing() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				" ",
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.MISSING_APPROVAL_POLICY);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.APPROVAL_POLICY);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				" ",
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.MISSING_OPERATOR_CONTEXT);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenDecisionRationaleRequirementMissing() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				false,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(
				ApprovalDecisionReason.MISSING_DECISION_RATIONALE_REQUIREMENT
		);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.DECISION_RATIONALE);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(decision.scope()).isEqualTo(ApprovalDecisionScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenApprovalStateIsPartial() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalStateWithStatus(ApprovalStateIntegrationStatus.PARTIAL_APPROVAL_STATE),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.PARTIAL);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.PARTIAL_APPROVAL_STATE);
	}

	@Test
	void shouldRemainNotReadyWhenApprovalStateIsNotReady() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalStateWithStatus(ApprovalStateIntegrationStatus.NOT_READY),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.NOT_READY);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.NOT_READY_APPROVAL_STATE);
	}

	@Test
	void shouldRemainUnreliableWhenApprovalStateIsUnreliable() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalStateWithStatus(ApprovalStateIntegrationStatus.UNRELIABLE),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.UNRELIABLE);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.UNRELIABLE_APPROVAL_STATE);
	}

	@Test
	void shouldRemainBlockedWhenApprovalStateIsBlocked() {
		ApprovalDecision decision = evaluator.evaluate(
				approvalStateWithStatus(ApprovalStateIntegrationStatus.BLOCKED),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(decision.level()).isEqualTo(ApprovalDecisionLevel.BLOCKED);
		assertThat(decision.reason()).isEqualTo(ApprovalDecisionReason.BLOCKED_APPROVAL_STATE);
	}

	@Test
	void shouldRejectNullApprovalStateIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalStateIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private ApprovalStateIntegrationResult approvalPendingView() {
		return approvalStateWithStatus(ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW);
	}

	private ApprovalStateIntegrationResult approvalStateWithStatus(
			ApprovalStateIntegrationStatus status
	) {
		return new ApprovalStateIntegrationResult(
				approvalState(status),
				status,
				approvalStateIntegrationReason(status),
				ApprovalStateIntegrationScope.OPERATOR_VIEW,
				status == ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW,
				status == ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW
		);
	}

	private ApprovalState approvalState(ApprovalStateIntegrationStatus status) {
		return new ApprovalState(
				approvalStateLevel(status),
				approvalStateReason(status),
				ApprovalStateScope.APPROVAL_REQUEST,
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalStateLevel approvalStateLevel(ApprovalStateIntegrationStatus status) {
		return switch (status) {
			case APPROVAL_PENDING_VIEW -> ApprovalStateLevel.PENDING_APPROVAL;
			case PARTIAL_APPROVAL_STATE -> ApprovalStateLevel.PARTIAL;
			case NOT_READY -> ApprovalStateLevel.NOT_READY;
			case UNRELIABLE -> ApprovalStateLevel.UNRELIABLE;
			case BLOCKED -> ApprovalStateLevel.BLOCKED;
			case UNKNOWN -> ApprovalStateLevel.UNKNOWN;
		};
	}

	private ApprovalStateReason approvalStateReason(ApprovalStateIntegrationStatus status) {
		return switch (status) {
			case APPROVAL_PENDING_VIEW -> ApprovalStateReason.APPROVAL_REQUEST_READY;
			case PARTIAL_APPROVAL_STATE -> ApprovalStateReason.PARTIAL_APPROVAL_REQUEST;
			case NOT_READY -> ApprovalStateReason.NOT_READY_APPROVAL_REQUEST;
			case UNRELIABLE -> ApprovalStateReason.UNRELIABLE_APPROVAL_REQUEST;
			case BLOCKED -> ApprovalStateReason.BLOCKED_APPROVAL_REQUEST;
			case UNKNOWN -> ApprovalStateReason.UNKNOWN;
		};
	}

	private ApprovalStateIntegrationReason approvalStateIntegrationReason(
			ApprovalStateIntegrationStatus status
	) {
		return switch (status) {
			case APPROVAL_PENDING_VIEW -> ApprovalStateIntegrationReason.PENDING_APPROVAL_STATE;
			case PARTIAL_APPROVAL_STATE -> ApprovalStateIntegrationReason.PARTIAL_APPROVAL_STATE;
			case NOT_READY -> ApprovalStateIntegrationReason.NOT_READY_APPROVAL_STATE;
			case UNRELIABLE -> ApprovalStateIntegrationReason.UNRELIABLE_APPROVAL_STATE;
			case BLOCKED -> ApprovalStateIntegrationReason.BLOCKED_APPROVAL_STATE;
			case UNKNOWN -> ApprovalStateIntegrationReason.UNKNOWN;
		};
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
