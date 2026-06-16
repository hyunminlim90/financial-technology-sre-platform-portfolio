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

class OperationalReliabilityApprovalStateIntegrationTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-15T00:00:00Z");

	private final ApprovalStateIntegration integration =
			new ApprovalStateIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalStateIntegrationResult result = integration.integrate(
				stateWithLevel(ApprovalStateLevel.PENDING_APPROVAL)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.humanApproval()).isFalse();
		assertThat(result.approvalDecision()).isFalse();
		assertThat(result.approvalWorkflow()).isFalse();
		assertThat(result.actionCommand()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeApprovalPendingViewWhenStateIsPendingApproval() {
		ApprovalStateIntegrationResult result = integration.integrate(
				stateWithLevel(ApprovalStateLevel.PENDING_APPROVAL)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW);
		assertThat(result.reason())
				.isEqualTo(ApprovalStateIntegrationReason.PENDING_APPROVAL_STATE);
		assertThat(result.scope())
				.isEqualTo(ApprovalStateIntegrationScope.OPERATOR_VIEW);
		assertThat(result.operatorFacingPendingApprovalVisible()).isTrue();
		assertThat(result.pendingApprovalCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRemainPartialWhenStateIsPartial() {
		ApprovalStateIntegrationResult result = integration.integrate(
				stateWithLevel(ApprovalStateLevel.PARTIAL)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalStateIntegrationStatus.PARTIAL_APPROVAL_STATE);
		assertThat(result.reason())
				.isEqualTo(ApprovalStateIntegrationReason.PARTIAL_APPROVAL_STATE);
	}

	@Test
	void shouldRemainNotReadyWhenStateIsNotReady() {
		ApprovalStateIntegrationResult result = integration.integrate(
				stateWithLevel(ApprovalStateLevel.NOT_READY)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalStateIntegrationStatus.NOT_READY);
		assertThat(result.reason())
				.isEqualTo(ApprovalStateIntegrationReason.NOT_READY_APPROVAL_STATE);
	}

	@Test
	void shouldRemainUnreliableWhenStateIsUnreliable() {
		ApprovalStateIntegrationResult result = integration.integrate(
				stateWithLevel(ApprovalStateLevel.UNRELIABLE)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalStateIntegrationStatus.UNRELIABLE);
		assertThat(result.reason())
				.isEqualTo(ApprovalStateIntegrationReason.UNRELIABLE_APPROVAL_STATE);
	}

	@Test
	void shouldRemainBlockedWhenStateIsBlocked() {
		ApprovalStateIntegrationResult result = integration.integrate(
				stateWithLevel(ApprovalStateLevel.BLOCKED)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalStateIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(ApprovalStateIntegrationReason.BLOCKED_APPROVAL_STATE);
	}

	@Test
	void shouldBlockWhenApprovalStateIdentifierMissing() {
		ApprovalStateIntegrationResult result = integration.integrate(
				new ApprovalState(
						ApprovalStateLevel.PENDING_APPROVAL,
						ApprovalStateReason.APPROVAL_REQUEST_READY,
						ApprovalStateScope.APPROVAL_REQUEST,
						approvalRequestReady(),
						" ",
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalStateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalStateIntegrationReason.MISSING_APPROVAL_STATE_IDENTIFIER
		);
		assertThat(result.scope()).isEqualTo(ApprovalStateIntegrationScope.APPROVAL_STATE);
	}

	@Test
	void shouldBlockWhenApprovalPolicyMissing() {
		ApprovalStateIntegrationResult result = integration.integrate(
				new ApprovalState(
						ApprovalStateLevel.PENDING_APPROVAL,
						ApprovalStateReason.APPROVAL_REQUEST_READY,
						ApprovalStateScope.APPROVAL_REQUEST,
						approvalRequestReady(),
						APPROVAL_STATE_IDENTIFIER,
						" ",
						OPERATOR_CONTEXT,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalStateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalStateIntegrationReason.MISSING_APPROVAL_POLICY
		);
		assertThat(result.scope()).isEqualTo(ApprovalStateIntegrationScope.APPROVAL_POLICY);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalStateIntegrationResult result = integration.integrate(
				new ApprovalState(
						ApprovalStateLevel.PENDING_APPROVAL,
						ApprovalStateReason.APPROVAL_REQUEST_READY,
						ApprovalStateScope.APPROVAL_REQUEST,
						approvalRequestReady(),
						APPROVAL_STATE_IDENTIFIER,
						APPROVAL_POLICY,
						" ",
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalStateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalStateIntegrationReason.MISSING_OPERATOR_CONTEXT
		);
		assertThat(result.scope()).isEqualTo(ApprovalStateIntegrationScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalStateIntegrationResult result = integration.integrate(
				new ApprovalState(
						ApprovalStateLevel.PENDING_APPROVAL,
						ApprovalStateReason.APPROVAL_REQUEST_READY,
						ApprovalStateScope.APPROVAL_REQUEST,
						approvalRequestReady(),
						APPROVAL_STATE_IDENTIFIER,
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						OperationalUncertainty.LOW,
						true
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalStateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalStateIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(ApprovalStateIntegrationScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalStateIntegrationResult result = integration.integrate(
				new ApprovalState(
						ApprovalStateLevel.PENDING_APPROVAL,
						ApprovalStateReason.APPROVAL_REQUEST_READY,
						ApprovalStateScope.APPROVAL_REQUEST,
						approvalRequestReady(),
						APPROVAL_STATE_IDENTIFIER,
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalStateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalStateIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(ApprovalStateIntegrationScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullApprovalState() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalState must not be null");
	}

	private ApprovalState stateWithLevel(ApprovalStateLevel level) {
		return new ApprovalState(
				level,
				stateReason(level),
				ApprovalStateScope.APPROVAL_REQUEST,
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalStateReason stateReason(ApprovalStateLevel level) {
		return switch (level) {
			case PENDING_APPROVAL -> ApprovalStateReason.APPROVAL_REQUEST_READY;
			case PARTIAL -> ApprovalStateReason.PARTIAL_APPROVAL_REQUEST;
			case NOT_READY -> ApprovalStateReason.NOT_READY_APPROVAL_REQUEST;
			case UNRELIABLE -> ApprovalStateReason.UNRELIABLE_APPROVAL_REQUEST;
			case BLOCKED -> ApprovalStateReason.BLOCKED_APPROVAL_REQUEST;
			case UNKNOWN -> ApprovalStateReason.UNKNOWN;
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
