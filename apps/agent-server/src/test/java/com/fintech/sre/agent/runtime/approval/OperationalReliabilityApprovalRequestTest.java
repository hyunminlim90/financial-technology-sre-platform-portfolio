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

class OperationalReliabilityApprovalRequestTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-15T00:00:00Z");

	private final ApprovalRequestEvaluator evaluator = new ApprovalRequestEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.readOnly()).isTrue();
		assertThat(request.approval()).isFalse();
		assertThat(request.humanApproval()).isFalse();
		assertThat(request.approvalWorkflow()).isFalse();
		assertThat(request.actionCommand()).isFalse();
		assertThat(request.executionPermission()).isFalse();
	}

	@Test
	void shouldBeRequestableWhenPresentationIsExposableAndApprovalContextExists() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.REQUESTABLE);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.EXPOSABLE_PRESENTATION);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.APPROVAL_REQUEST);
	}

	@Test
	void shouldBePartialWhenPresentationIsPartial() {
		ApprovalRequest request = evaluator.evaluate(
				presentationWithStatus(RecommendationPresentationIntegrationStatus.PARTIAL),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.PARTIAL);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.PARTIAL_PRESENTATION);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.PRESENTATION);
	}

	@Test
	void shouldBeNotReadyWhenPresentationIsNotReady() {
		ApprovalRequest request = evaluator.evaluate(
				presentationWithStatus(RecommendationPresentationIntegrationStatus.NOT_READY),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.NOT_READY);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.NOT_READY_PRESENTATION);
	}

	@Test
	void shouldBeUnreliableWhenPresentationIsUnreliable() {
		ApprovalRequest request = evaluator.evaluate(
				presentationWithStatus(RecommendationPresentationIntegrationStatus.UNRELIABLE),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.UNRELIABLE);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.UNRELIABLE_PRESENTATION);
	}

	@Test
	void shouldBeBlockedWhenPresentationIsBlocked() {
		ApprovalRequest request = evaluator.evaluate(
				presentationWithStatus(RecommendationPresentationIntegrationStatus.BLOCKED),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.BLOCKED_PRESENTATION);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				" ",
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.MISSING_OPERATOR_CONTEXT);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenHumanApprovalRequirementMissing() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				false,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.MISSING_HUMAN_APPROVAL_REQUIREMENT);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldBlockWhenApprovalPolicyMissing() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				" ",
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.MISSING_APPROVAL_POLICY);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.APPROVAL_POLICY);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalRequest request = evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(request.level()).isEqualTo(ApprovalRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(ApprovalRequestReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(request.scope()).isEqualTo(ApprovalRequestScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullPresentationIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("presentationIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private RecommendationPresentationIntegrationResult exposablePresentation() {
		return presentationWithStatus(RecommendationPresentationIntegrationStatus.EXPOSABLE);
	}

	private RecommendationPresentationIntegrationResult presentationWithStatus(
			RecommendationPresentationIntegrationStatus status
	) {
		return new RecommendationPresentationIntegrationResult(
				presentation(),
				status,
				integrationReason(status),
				RecommendationPresentationIntegrationScope.RECOMMENDATION,
				status == RecommendationPresentationIntegrationStatus.EXPOSABLE,
				status == RecommendationPresentationIntegrationStatus.EXPOSABLE
		);
	}

	private RecommendationPresentationIntegrationReason integrationReason(
			RecommendationPresentationIntegrationStatus status
	) {
		return switch (status) {
			case EXPOSABLE -> RecommendationPresentationIntegrationReason.VALID_RECOMMENDATION_PRESENTATION;
			case PARTIAL -> RecommendationPresentationIntegrationReason.PARTIAL_PRESENTATION;
			case NOT_READY -> RecommendationPresentationIntegrationReason.NOT_PRESENTABLE_RECOMMENDATION;
			case UNRELIABLE -> RecommendationPresentationIntegrationReason.UNRELIABLE_RECOMMENDATION;
			case BLOCKED -> RecommendationPresentationIntegrationReason.BLOCKED_RECOMMENDATION;
			case UNKNOWN -> RecommendationPresentationIntegrationReason.UNKNOWN;
		};
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
