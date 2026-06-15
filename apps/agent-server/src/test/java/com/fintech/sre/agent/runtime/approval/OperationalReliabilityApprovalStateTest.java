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

class OperationalReliabilityApprovalStateTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-15T00:00:00Z");

	private final ApprovalStateEvaluator evaluator = new ApprovalStateEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.readOnly()).isTrue();
		assertThat(state.humanApprovalCompleted()).isFalse();
		assertThat(state.approvalDecision()).isFalse();
		assertThat(state.verificationRequest()).isFalse();
		assertThat(state.actionCommand()).isFalse();
		assertThat(state.executionPermission()).isFalse();
	}

	@Test
	void shouldBePendingApprovalWhenApprovalRequestIsReady() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.PENDING_APPROVAL);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.APPROVAL_REQUEST_READY);
		assertThat(state.scope()).isEqualTo(ApprovalStateScope.APPROVAL_REQUEST);
	}

	@Test
	void shouldBlockWhenApprovalStateIdentifierMissing() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				" ",
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.BLOCKED);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.MISSING_APPROVAL_STATE_IDENTIFIER);
		assertThat(state.scope()).isEqualTo(ApprovalStateScope.APPROVAL_STATE);
	}

	@Test
	void shouldBlockWhenApprovalPolicyMissing() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				" ",
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.BLOCKED);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.MISSING_APPROVAL_POLICY);
		assertThat(state.scope()).isEqualTo(ApprovalStateScope.APPROVAL_POLICY);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				" ",
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.BLOCKED);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.MISSING_OPERATOR_CONTEXT);
		assertThat(state.scope()).isEqualTo(ApprovalStateScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.BLOCKED);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(state.scope()).isEqualTo(ApprovalStateScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.BLOCKED);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(state.scope()).isEqualTo(ApprovalStateScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenApprovalRequestIsPartial() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestWithStatus(ApprovalRequestIntegrationStatus.PARTIAL_APPROVAL_REQUEST),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.PARTIAL);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.PARTIAL_APPROVAL_REQUEST);
	}

	@Test
	void shouldRemainNotReadyWhenApprovalRequestIsNotReady() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestWithStatus(ApprovalRequestIntegrationStatus.NOT_READY),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.NOT_READY);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.NOT_READY_APPROVAL_REQUEST);
	}

	@Test
	void shouldRemainUnreliableWhenApprovalRequestIsUnreliable() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestWithStatus(ApprovalRequestIntegrationStatus.UNRELIABLE),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.UNRELIABLE);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.UNRELIABLE_APPROVAL_REQUEST);
	}

	@Test
	void shouldRemainBlockedWhenApprovalRequestIsBlocked() {
		ApprovalState state = evaluator.evaluate(
				approvalRequestWithStatus(ApprovalRequestIntegrationStatus.BLOCKED),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(state.level()).isEqualTo(ApprovalStateLevel.BLOCKED);
		assertThat(state.reason()).isEqualTo(ApprovalStateReason.BLOCKED_APPROVAL_REQUEST);
	}

	@Test
	void shouldRejectNullApprovalRequestIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalRequestIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private ApprovalRequestIntegrationResult approvalRequestReady() {
		return approvalRequestWithStatus(ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY);
	}

	private ApprovalRequestIntegrationResult approvalRequestWithStatus(
			ApprovalRequestIntegrationStatus status
	) {
		return new ApprovalRequestIntegrationResult(
				approvalRequest(status),
				status,
				integrationReason(status),
				requestIntegrationScope(status),
				status == ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY,
				status == ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY
		);
	}

	private ApprovalRequest approvalRequest(ApprovalRequestIntegrationStatus status) {
		return new ApprovalRequest(
				requestLevel(status),
				requestReason(status),
				requestScope(status),
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalRequestLevel requestLevel(ApprovalRequestIntegrationStatus status) {
		return switch (status) {
			case APPROVAL_REQUEST_READY -> ApprovalRequestLevel.REQUESTABLE;
			case PARTIAL_APPROVAL_REQUEST -> ApprovalRequestLevel.PARTIAL;
			case NOT_READY -> ApprovalRequestLevel.NOT_READY;
			case UNRELIABLE -> ApprovalRequestLevel.UNRELIABLE;
			case BLOCKED -> ApprovalRequestLevel.BLOCKED;
			case UNKNOWN -> ApprovalRequestLevel.UNKNOWN;
		};
	}

	private ApprovalRequestReason requestReason(ApprovalRequestIntegrationStatus status) {
		return switch (status) {
			case APPROVAL_REQUEST_READY -> ApprovalRequestReason.EXPOSABLE_PRESENTATION;
			case PARTIAL_APPROVAL_REQUEST -> ApprovalRequestReason.PARTIAL_PRESENTATION;
			case NOT_READY -> ApprovalRequestReason.NOT_READY_PRESENTATION;
			case UNRELIABLE -> ApprovalRequestReason.UNRELIABLE_PRESENTATION;
			case BLOCKED -> ApprovalRequestReason.BLOCKED_PRESENTATION;
			case UNKNOWN -> ApprovalRequestReason.UNKNOWN;
		};
	}

	private ApprovalRequestScope requestScope(ApprovalRequestIntegrationStatus status) {
		return status == ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY
				? ApprovalRequestScope.APPROVAL_REQUEST
				: ApprovalRequestScope.PRESENTATION;
	}

	private ApprovalRequestIntegrationReason integrationReason(
			ApprovalRequestIntegrationStatus status
	) {
		return switch (status) {
			case APPROVAL_REQUEST_READY -> ApprovalRequestIntegrationReason.REQUESTABLE_APPROVAL_REQUEST;
			case PARTIAL_APPROVAL_REQUEST -> ApprovalRequestIntegrationReason.PARTIAL_APPROVAL_REQUEST;
			case NOT_READY -> ApprovalRequestIntegrationReason.NOT_READY_APPROVAL_REQUEST;
			case UNRELIABLE -> ApprovalRequestIntegrationReason.UNRELIABLE_APPROVAL_REQUEST;
			case BLOCKED -> ApprovalRequestIntegrationReason.BLOCKED_APPROVAL_REQUEST;
			case UNKNOWN -> ApprovalRequestIntegrationReason.UNKNOWN;
		};
	}

	private ApprovalRequestIntegrationScope requestIntegrationScope(
			ApprovalRequestIntegrationStatus status
	) {
		return status == ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY
				? ApprovalRequestIntegrationScope.APPROVAL_REQUEST
				: ApprovalRequestIntegrationScope.OPERATOR_VIEW;
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
