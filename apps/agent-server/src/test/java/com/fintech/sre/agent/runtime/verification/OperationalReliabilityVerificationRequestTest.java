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

class OperationalReliabilityVerificationRequestTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String DECISION_IDENTIFIER = "approval-decision/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final String VERIFICATION_REQUEST_IDENTIFIER = "verification-request/payments/001";
	private static final String VERIFICATION_POLICY = "policy/post-change-verification";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-16T00:00:00Z");

	private final VerificationRequestEvaluator evaluator =
			new VerificationRequestEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.readOnly()).isTrue();
		assertThat(request.verificationExecution()).isFalse();
		assertThat(request.verificationResult()).isFalse();
		assertThat(request.verificationWorkflow()).isFalse();
		assertThat(request.actionCommand()).isFalse();
		assertThat(request.executionPermission()).isFalse();
	}

	@Test
	void shouldBeVerificationRequestableWhenApprovalDecisionPendingViewExists() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.VERIFICATION_REQUESTABLE);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.APPROVAL_DECISION);
	}

	@Test
	void shouldBlockWhenVerificationRequestIdentifierMissing() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				" ",
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(
				VerificationRequestReason.MISSING_VERIFICATION_REQUEST_IDENTIFIER
		);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.VERIFICATION_REQUEST);
	}

	@Test
	void shouldBlockWhenVerificationPolicyMissing() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				" ",
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.MISSING_VERIFICATION_POLICY);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.VERIFICATION_POLICY);
	}

	@Test
	void shouldBlockWhenVerificationEvidenceRequirementMissing() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				false,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(
				VerificationRequestReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT
		);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.VERIFICATION_EVIDENCE);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				false,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.MISSING_ROLLBACK_BINDING);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.ROLLBACK);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(request.scope()).isEqualTo(VerificationRequestScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenApprovalDecisionIsPartial() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionWithStatus(ApprovalDecisionIntegrationStatus.PARTIAL_APPROVAL_DECISION),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.PARTIAL);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.PARTIAL_APPROVAL_DECISION);
	}

	@Test
	void shouldRemainNotReadyWhenApprovalDecisionIsNotReady() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionWithStatus(ApprovalDecisionIntegrationStatus.NOT_READY),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.NOT_READY);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.NOT_READY_APPROVAL_DECISION);
	}

	@Test
	void shouldRemainUnreliableWhenApprovalDecisionIsUnreliable() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionWithStatus(ApprovalDecisionIntegrationStatus.UNRELIABLE),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.UNRELIABLE);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.UNRELIABLE_APPROVAL_DECISION);
	}

	@Test
	void shouldRemainBlockedWhenApprovalDecisionIsBlocked() {
		VerificationRequest request = evaluator.evaluate(
				approvalDecisionWithStatus(ApprovalDecisionIntegrationStatus.BLOCKED),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(request.level()).isEqualTo(VerificationRequestLevel.BLOCKED);
		assertThat(request.reason()).isEqualTo(VerificationRequestReason.BLOCKED_APPROVAL_DECISION);
	}

	@Test
	void shouldRejectNullApprovalDecisionIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("approvalDecisionIntegration must not be null");
	}

	@Test
	void shouldRejectNullLifecycleRisk() {
		assertThatThrownBy(() -> evaluator.evaluate(
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private ApprovalDecisionIntegrationResult approvalDecisionPendingView() {
		return approvalDecisionWithStatus(
				ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW
		);
	}

	private ApprovalDecisionIntegrationResult approvalDecisionWithStatus(
			ApprovalDecisionIntegrationStatus status
	) {
		return new ApprovalDecisionIntegrationResult(
				approvalDecision(status),
				status,
				approvalDecisionIntegrationReason(status),
				ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
				status == ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW,
				status == ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW
		);
	}

	private ApprovalDecision approvalDecision(ApprovalDecisionIntegrationStatus status) {
		return new ApprovalDecision(
				approvalDecisionLevel(status),
				approvalDecisionReason(status),
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

	private ApprovalDecisionLevel approvalDecisionLevel(
			ApprovalDecisionIntegrationStatus status
	) {
		return switch (status) {
			case APPROVAL_DECISION_PENDING_VIEW -> ApprovalDecisionLevel.DECISION_PENDING;
			case PARTIAL_APPROVAL_DECISION -> ApprovalDecisionLevel.PARTIAL;
			case NOT_READY -> ApprovalDecisionLevel.NOT_READY;
			case UNRELIABLE -> ApprovalDecisionLevel.UNRELIABLE;
			case BLOCKED -> ApprovalDecisionLevel.BLOCKED;
			case UNKNOWN -> ApprovalDecisionLevel.UNKNOWN;
		};
	}

	private ApprovalDecisionReason approvalDecisionReason(
			ApprovalDecisionIntegrationStatus status
	) {
		return switch (status) {
			case APPROVAL_DECISION_PENDING_VIEW -> ApprovalDecisionReason.APPROVAL_PENDING_VIEW;
			case PARTIAL_APPROVAL_DECISION -> ApprovalDecisionReason.PARTIAL_APPROVAL_STATE;
			case NOT_READY -> ApprovalDecisionReason.NOT_READY_APPROVAL_STATE;
			case UNRELIABLE -> ApprovalDecisionReason.UNRELIABLE_APPROVAL_STATE;
			case BLOCKED -> ApprovalDecisionReason.BLOCKED_APPROVAL_STATE;
			case UNKNOWN -> ApprovalDecisionReason.UNKNOWN;
		};
	}

	private ApprovalDecisionIntegrationReason approvalDecisionIntegrationReason(
			ApprovalDecisionIntegrationStatus status
	) {
		return switch (status) {
			case APPROVAL_DECISION_PENDING_VIEW -> ApprovalDecisionIntegrationReason.DECISION_PENDING;
			case PARTIAL_APPROVAL_DECISION -> ApprovalDecisionIntegrationReason.PARTIAL_APPROVAL_DECISION;
			case NOT_READY -> ApprovalDecisionIntegrationReason.NOT_READY_APPROVAL_DECISION;
			case UNRELIABLE -> ApprovalDecisionIntegrationReason.UNRELIABLE_APPROVAL_DECISION;
			case BLOCKED -> ApprovalDecisionIntegrationReason.BLOCKED_APPROVAL_DECISION;
			case UNKNOWN -> ApprovalDecisionIntegrationReason.UNKNOWN;
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
