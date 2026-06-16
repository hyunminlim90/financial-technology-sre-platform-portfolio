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

class OperationalReliabilityApprovalDecisionIntegrationTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String DECISION_IDENTIFIER = "approval-decision/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-15T00:00:00Z");

	private final ApprovalDecisionIntegration integration =
			new ApprovalDecisionIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				decisionWithLevel(ApprovalDecisionLevel.DECISION_PENDING)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.humanApproval()).isFalse();
		assertThat(result.approvalResult()).isFalse();
		assertThat(result.approvalWorkflow()).isFalse();
		assertThat(result.verificationRequest()).isFalse();
		assertThat(result.actionCommand()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeApprovalDecisionPendingViewWhenDecisionIsPending() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				decisionWithLevel(ApprovalDecisionLevel.DECISION_PENDING)
		);

		assertThat(result.status())
				.isEqualTo(
						ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW
				);
		assertThat(result.reason())
				.isEqualTo(ApprovalDecisionIntegrationReason.DECISION_PENDING);
		assertThat(result.scope())
				.isEqualTo(ApprovalDecisionIntegrationScope.OPERATOR_VIEW);
		assertThat(result.operatorFacingDecisionPendingVisible()).isTrue();
		assertThat(result.decisionPendingCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRemainPartialWhenDecisionIsPartial() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				decisionWithLevel(ApprovalDecisionLevel.PARTIAL)
		);

		assertThat(result.status())
				.isEqualTo(
						ApprovalDecisionIntegrationStatus.PARTIAL_APPROVAL_DECISION
				);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalDecisionIntegrationReason.PARTIAL_APPROVAL_DECISION
				);
	}

	@Test
	void shouldRemainNotReadyWhenDecisionIsNotReady() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				decisionWithLevel(ApprovalDecisionLevel.NOT_READY)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalDecisionIntegrationStatus.NOT_READY);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalDecisionIntegrationReason.NOT_READY_APPROVAL_DECISION
				);
	}

	@Test
	void shouldRemainUnreliableWhenDecisionIsUnreliable() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				decisionWithLevel(ApprovalDecisionLevel.UNRELIABLE)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalDecisionIntegrationStatus.UNRELIABLE);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalDecisionIntegrationReason.UNRELIABLE_APPROVAL_DECISION
				);
	}

	@Test
	void shouldRemainBlockedWhenDecisionIsBlocked() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				decisionWithLevel(ApprovalDecisionLevel.BLOCKED)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalDecisionIntegrationReason.BLOCKED_APPROVAL_DECISION
				);
	}

	@Test
	void shouldBlockWhenDecisionIdentifierMissing() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				new ApprovalDecision(
						ApprovalDecisionLevel.DECISION_PENDING,
						ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
						ApprovalDecisionScope.APPROVAL_STATE,
						approvalPendingView(),
						" ",
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalDecisionIntegrationReason.MISSING_DECISION_IDENTIFIER
		);
		assertThat(result.scope()).isEqualTo(ApprovalDecisionIntegrationScope.APPROVAL_DECISION);
	}

	@Test
	void shouldBlockWhenApprovalPolicyMissing() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				new ApprovalDecision(
						ApprovalDecisionLevel.DECISION_PENDING,
						ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
						ApprovalDecisionScope.APPROVAL_STATE,
						approvalPendingView(),
						DECISION_IDENTIFIER,
						" ",
						OPERATOR_CONTEXT,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalDecisionIntegrationReason.MISSING_APPROVAL_POLICY
		);
		assertThat(result.scope()).isEqualTo(ApprovalDecisionIntegrationScope.APPROVAL_POLICY);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				new ApprovalDecision(
						ApprovalDecisionLevel.DECISION_PENDING,
						ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
						ApprovalDecisionScope.APPROVAL_STATE,
						approvalPendingView(),
						DECISION_IDENTIFIER,
						APPROVAL_POLICY,
						" ",
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalDecisionIntegrationReason.MISSING_OPERATOR_CONTEXT
		);
		assertThat(result.scope()).isEqualTo(ApprovalDecisionIntegrationScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenDecisionRationaleRequirementMissing() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				new ApprovalDecision(
						ApprovalDecisionLevel.DECISION_PENDING,
						ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
						ApprovalDecisionScope.APPROVAL_STATE,
						approvalPendingView(),
						DECISION_IDENTIFIER,
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						false,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalDecisionIntegrationReason.MISSING_DECISION_RATIONALE_REQUIREMENT
		);
		assertThat(result.scope()).isEqualTo(ApprovalDecisionIntegrationScope.DECISION_RATIONALE);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				new ApprovalDecision(
						ApprovalDecisionLevel.DECISION_PENDING,
						ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
						ApprovalDecisionScope.APPROVAL_STATE,
						approvalPendingView(),
						DECISION_IDENTIFIER,
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						true,
						OperationalUncertainty.LOW,
						true
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalDecisionIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(ApprovalDecisionIntegrationScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalDecisionIntegrationResult result = integration.integrate(
				new ApprovalDecision(
						ApprovalDecisionLevel.DECISION_PENDING,
						ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
						ApprovalDecisionScope.APPROVAL_STATE,
						approvalPendingView(),
						DECISION_IDENTIFIER,
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						true,
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalDecisionIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalDecisionIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(ApprovalDecisionIntegrationScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullApprovalDecision() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalDecision must not be null");
	}

	private ApprovalDecision decisionWithLevel(ApprovalDecisionLevel level) {
		return new ApprovalDecision(
				level,
				decisionReason(level),
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

	private ApprovalDecisionReason decisionReason(ApprovalDecisionLevel level) {
		return switch (level) {
			case DECISION_PENDING -> ApprovalDecisionReason.APPROVAL_PENDING_VIEW;
			case PARTIAL -> ApprovalDecisionReason.PARTIAL_APPROVAL_STATE;
			case NOT_READY -> ApprovalDecisionReason.NOT_READY_APPROVAL_STATE;
			case UNRELIABLE -> ApprovalDecisionReason.UNRELIABLE_APPROVAL_STATE;
			case BLOCKED -> ApprovalDecisionReason.BLOCKED_APPROVAL_STATE;
			case UNKNOWN -> ApprovalDecisionReason.UNKNOWN;
		};
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
