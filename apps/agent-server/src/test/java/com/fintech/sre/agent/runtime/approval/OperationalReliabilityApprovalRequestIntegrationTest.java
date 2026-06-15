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

class OperationalReliabilityApprovalRequestIntegrationTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-15T00:00:00Z");

	private final ApprovalRequestIntegration integration =
			new ApprovalRequestIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				requestWithLevel(ApprovalRequestLevel.REQUESTABLE)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.approvalRequestGeneration()).isFalse();
		assertThat(result.approvalWorkflow()).isFalse();
		assertThat(result.humanApproval()).isFalse();
		assertThat(result.actionCommand()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeApprovalRequestReadyWhenApprovalRequestIsRequestable() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				requestWithLevel(ApprovalRequestLevel.REQUESTABLE)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalRequestIntegrationReason.REQUESTABLE_APPROVAL_REQUEST
				);
		assertThat(result.scope())
				.isEqualTo(ApprovalRequestIntegrationScope.APPROVAL_REQUEST);
		assertThat(result.workflowEntryReady()).isTrue();
		assertThat(result.requestCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldBePartialWhenApprovalRequestIsPartial() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				requestWithLevel(ApprovalRequestLevel.PARTIAL)
		);

		assertThat(result.status())
				.isEqualTo(
						ApprovalRequestIntegrationStatus.PARTIAL_APPROVAL_REQUEST
				);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalRequestIntegrationReason.PARTIAL_APPROVAL_REQUEST
				);
	}

	@Test
	void shouldBeNotReadyWhenApprovalRequestIsNotReady() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				requestWithLevel(ApprovalRequestLevel.NOT_READY)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalRequestIntegrationStatus.NOT_READY);
		assertThat(result.reason())
				.isEqualTo(ApprovalRequestIntegrationReason.NOT_READY_APPROVAL_REQUEST);
	}

	@Test
	void shouldBeUnreliableWhenApprovalRequestIsUnreliable() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				requestWithLevel(ApprovalRequestLevel.UNRELIABLE)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalRequestIntegrationStatus.UNRELIABLE);
		assertThat(result.reason())
				.isEqualTo(
						ApprovalRequestIntegrationReason.UNRELIABLE_APPROVAL_REQUEST
				);
	}

	@Test
	void shouldBeBlockedWhenApprovalRequestIsBlocked() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				requestWithLevel(ApprovalRequestLevel.BLOCKED)
		);

		assertThat(result.status())
				.isEqualTo(ApprovalRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(ApprovalRequestIntegrationReason.BLOCKED_APPROVAL_REQUEST);
	}

	@Test
	void shouldBlockWhenOperatorContextMissing() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				new ApprovalRequest(
						ApprovalRequestLevel.REQUESTABLE,
						ApprovalRequestReason.EXPOSABLE_PRESENTATION,
						ApprovalRequestScope.APPROVAL_REQUEST,
						exposablePresentation(),
						" ",
						true,
						APPROVAL_POLICY,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(ApprovalRequestIntegrationReason.MISSING_OPERATOR_CONTEXT);
		assertThat(result.scope()).isEqualTo(ApprovalRequestIntegrationScope.OPERATOR_CONTEXT);
	}

	@Test
	void shouldBlockWhenHumanApprovalRequirementMissing() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				new ApprovalRequest(
						ApprovalRequestLevel.REQUESTABLE,
						ApprovalRequestReason.EXPOSABLE_PRESENTATION,
						ApprovalRequestScope.APPROVAL_REQUEST,
						exposablePresentation(),
						OPERATOR_CONTEXT,
						false,
						APPROVAL_POLICY,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalRequestIntegrationReason.MISSING_HUMAN_APPROVAL_REQUIREMENT
		);
		assertThat(result.scope()).isEqualTo(ApprovalRequestIntegrationScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldBlockWhenApprovalPolicyMissing() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				new ApprovalRequest(
						ApprovalRequestLevel.REQUESTABLE,
						ApprovalRequestReason.EXPOSABLE_PRESENTATION,
						ApprovalRequestScope.APPROVAL_REQUEST,
						exposablePresentation(),
						OPERATOR_CONTEXT,
						true,
						" ",
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalRequestIntegrationReason.MISSING_APPROVAL_POLICY
		);
		assertThat(result.scope()).isEqualTo(ApprovalRequestIntegrationScope.APPROVAL_POLICY);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				new ApprovalRequest(
						ApprovalRequestLevel.REQUESTABLE,
						ApprovalRequestReason.EXPOSABLE_PRESENTATION,
						ApprovalRequestScope.APPROVAL_REQUEST,
						exposablePresentation(),
						OPERATOR_CONTEXT,
						true,
						APPROVAL_POLICY,
						OperationalUncertainty.LOW,
						true
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalRequestIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(ApprovalRequestIntegrationScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ApprovalRequestIntegrationResult result = integration.integrate(
				new ApprovalRequest(
						ApprovalRequestLevel.REQUESTABLE,
						ApprovalRequestReason.EXPOSABLE_PRESENTATION,
						ApprovalRequestScope.APPROVAL_REQUEST,
						exposablePresentation(),
						OPERATOR_CONTEXT,
						true,
						APPROVAL_POLICY,
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(ApprovalRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				ApprovalRequestIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(ApprovalRequestIntegrationScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullApprovalRequest() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalRequest must not be null");
	}

	private ApprovalRequest requestWithLevel(ApprovalRequestLevel level) {
		return new ApprovalRequest(
				level,
				requestReason(level),
				requestScope(level),
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalRequestReason requestReason(ApprovalRequestLevel level) {
		return switch (level) {
			case REQUESTABLE -> ApprovalRequestReason.EXPOSABLE_PRESENTATION;
			case PARTIAL -> ApprovalRequestReason.PARTIAL_PRESENTATION;
			case NOT_READY -> ApprovalRequestReason.NOT_READY_PRESENTATION;
			case UNRELIABLE -> ApprovalRequestReason.UNRELIABLE_PRESENTATION;
			case BLOCKED -> ApprovalRequestReason.BLOCKED_PRESENTATION;
			case UNKNOWN -> ApprovalRequestReason.UNKNOWN;
		};
	}

	private ApprovalRequestScope requestScope(ApprovalRequestLevel level) {
		return level == ApprovalRequestLevel.REQUESTABLE
				? ApprovalRequestScope.APPROVAL_REQUEST
				: ApprovalRequestScope.PRESENTATION;
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
