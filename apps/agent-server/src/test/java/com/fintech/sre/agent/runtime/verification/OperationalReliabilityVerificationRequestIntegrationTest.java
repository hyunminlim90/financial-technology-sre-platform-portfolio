package com.fintech.sre.agent.runtime.verification;

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

class OperationalReliabilityVerificationRequestIntegrationTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String DECISION_IDENTIFIER = "approval-decision/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final String VERIFICATION_REQUEST_IDENTIFIER = "verification-request/payments/001";
	private static final String VERIFICATION_POLICY = "policy/post-change-verification";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-18T00:00:00Z");

	private final VerificationRequestIntegration integration =
			new VerificationRequestIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		VerificationRequestIntegrationResult result = integration.integrate(
				requestWithLevel(VerificationRequestLevel.VERIFICATION_REQUESTABLE)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.verificationRequestGeneration()).isFalse();
		assertThat(result.verificationWorkflow()).isFalse();
		assertThat(result.verificationResult()).isFalse();
		assertThat(result.actionCommand()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeVerificationRequestReadyWhenRequestIsRequestable() {
		VerificationRequestIntegrationResult result = integration.integrate(
				requestWithLevel(VerificationRequestLevel.VERIFICATION_REQUESTABLE)
		);

		assertThat(result.status())
				.isEqualTo(VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY);
		assertThat(result.reason())
				.isEqualTo(VerificationRequestIntegrationReason.VERIFICATION_REQUESTABLE);
		assertThat(result.scope())
				.isEqualTo(VerificationRequestIntegrationScope.OPERATOR_VIEW);
		assertThat(result.workflowEntryReady()).isTrue();
		assertThat(result.verificationRequestCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRemainPartialWhenRequestIsPartial() {
		VerificationRequestIntegrationResult result = integration.integrate(
				requestWithLevel(VerificationRequestLevel.PARTIAL)
		);

		assertThat(result.status())
				.isEqualTo(
						VerificationRequestIntegrationStatus.PARTIAL_VERIFICATION_REQUEST
				);
		assertThat(result.reason())
				.isEqualTo(
						VerificationRequestIntegrationReason.PARTIAL_VERIFICATION_REQUEST
				);
	}

	@Test
	void shouldRemainNotReadyWhenRequestIsNotReady() {
		VerificationRequestIntegrationResult result = integration.integrate(
				requestWithLevel(VerificationRequestLevel.NOT_READY)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.NOT_READY);
		assertThat(result.reason())
				.isEqualTo(VerificationRequestIntegrationReason.NOT_READY_VERIFICATION_REQUEST);
	}

	@Test
	void shouldRemainUnreliableWhenRequestIsUnreliable() {
		VerificationRequestIntegrationResult result = integration.integrate(
				requestWithLevel(VerificationRequestLevel.UNRELIABLE)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.UNRELIABLE);
		assertThat(result.reason())
				.isEqualTo(VerificationRequestIntegrationReason.UNRELIABLE_VERIFICATION_REQUEST);
	}

	@Test
	void shouldRemainBlockedWhenRequestIsBlocked() {
		VerificationRequestIntegrationResult result = integration.integrate(
				requestWithLevel(VerificationRequestLevel.BLOCKED)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(VerificationRequestIntegrationReason.BLOCKED_VERIFICATION_REQUEST);
	}

	@Test
	void shouldBlockWhenVerificationRequestIdentifierMissing() {
		VerificationRequestIntegrationResult result = integration.integrate(
				new VerificationRequest(
						VerificationRequestLevel.VERIFICATION_REQUESTABLE,
						VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
						VerificationRequestScope.APPROVAL_DECISION,
						approvalDecisionPendingView(),
						" ",
						VERIFICATION_POLICY,
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				VerificationRequestIntegrationReason.MISSING_VERIFICATION_REQUEST_IDENTIFIER
		);
		assertThat(result.scope()).isEqualTo(VerificationRequestIntegrationScope.VERIFICATION_REQUEST);
	}

	@Test
	void shouldBlockWhenVerificationPolicyMissing() {
		VerificationRequestIntegrationResult result = integration.integrate(
				new VerificationRequest(
						VerificationRequestLevel.VERIFICATION_REQUESTABLE,
						VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
						VerificationRequestScope.APPROVAL_DECISION,
						approvalDecisionPendingView(),
						VERIFICATION_REQUEST_IDENTIFIER,
						" ",
						true,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				VerificationRequestIntegrationReason.MISSING_VERIFICATION_POLICY
		);
		assertThat(result.scope()).isEqualTo(VerificationRequestIntegrationScope.VERIFICATION_POLICY);
	}

	@Test
	void shouldBlockWhenVerificationEvidenceRequirementMissing() {
		VerificationRequestIntegrationResult result = integration.integrate(
				new VerificationRequest(
						VerificationRequestLevel.VERIFICATION_REQUESTABLE,
						VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
						VerificationRequestScope.APPROVAL_DECISION,
						approvalDecisionPendingView(),
						VERIFICATION_REQUEST_IDENTIFIER,
						VERIFICATION_POLICY,
						false,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				VerificationRequestIntegrationReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT
		);
		assertThat(result.scope()).isEqualTo(VerificationRequestIntegrationScope.VERIFICATION_EVIDENCE);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		VerificationRequestIntegrationResult result = integration.integrate(
				new VerificationRequest(
						VerificationRequestLevel.VERIFICATION_REQUESTABLE,
						VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
						VerificationRequestScope.APPROVAL_DECISION,
						approvalDecisionPendingView(),
						VERIFICATION_REQUEST_IDENTIFIER,
						VERIFICATION_POLICY,
						true,
						false,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				VerificationRequestIntegrationReason.MISSING_ROLLBACK_BINDING
		);
		assertThat(result.scope()).isEqualTo(VerificationRequestIntegrationScope.ROLLBACK);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		VerificationRequestIntegrationResult result = integration.integrate(
				new VerificationRequest(
						VerificationRequestLevel.VERIFICATION_REQUESTABLE,
						VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
						VerificationRequestScope.APPROVAL_DECISION,
						approvalDecisionPendingView(),
						VERIFICATION_REQUEST_IDENTIFIER,
						VERIFICATION_POLICY,
						true,
						true,
						OperationalUncertainty.LOW,
						true
				)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				VerificationRequestIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(VerificationRequestIntegrationScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		VerificationRequestIntegrationResult result = integration.integrate(
				new VerificationRequest(
						VerificationRequestLevel.VERIFICATION_REQUESTABLE,
						VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
						VerificationRequestScope.APPROVAL_DECISION,
						approvalDecisionPendingView(),
						VERIFICATION_REQUEST_IDENTIFIER,
						VERIFICATION_POLICY,
						true,
						true,
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(VerificationRequestIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				VerificationRequestIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(VerificationRequestIntegrationScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRejectNullVerificationRequest() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("verificationRequest must not be null");
	}

	private VerificationRequest requestWithLevel(VerificationRequestLevel level) {
		return new VerificationRequest(
				level,
				requestReason(level),
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

	private VerificationRequestReason requestReason(VerificationRequestLevel level) {
		return switch (level) {
			case VERIFICATION_REQUESTABLE -> VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW;
			case PARTIAL -> VerificationRequestReason.PARTIAL_APPROVAL_DECISION;
			case NOT_READY -> VerificationRequestReason.NOT_READY_APPROVAL_DECISION;
			case UNRELIABLE -> VerificationRequestReason.UNRELIABLE_APPROVAL_DECISION;
			case BLOCKED -> VerificationRequestReason.BLOCKED_APPROVAL_DECISION;
			case UNKNOWN -> VerificationRequestReason.UNKNOWN;
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
