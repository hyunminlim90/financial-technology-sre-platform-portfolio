package com.fintech.sre.agent.runtime.approval;

import java.util.Objects;

import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationResult;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ApprovalRequestEvaluator {

	public ApprovalRequest evaluate(
			RecommendationPresentationIntegrationResult presentationIntegration,
			String operatorContext,
			boolean humanApprovalRequired,
			String approvalPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				presentationIntegration,
				"presentationIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ApprovalRequest(
				level(
						presentationIntegration,
						operatorContext,
						humanApprovalRequired,
						approvalPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						presentationIntegration,
						operatorContext,
						humanApprovalRequired,
						approvalPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						presentationIntegration,
						operatorContext,
						humanApprovalRequired,
						approvalPolicy,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				presentationIntegration,
				operatorContext,
				humanApprovalRequired,
				approvalPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ApprovalRequestLevel level(
			RecommendationPresentationIntegrationResult presentationIntegration,
			String operatorContext,
			boolean humanApprovalRequired,
			String approvalPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalRequestLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalRequestLevel.BLOCKED;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalRequestLevel.BLOCKED;
		}
		if (!humanApprovalRequired) {
			return ApprovalRequestLevel.BLOCKED;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalRequestLevel.BLOCKED;
		}
		if (presentationIntegration.status()
				== RecommendationPresentationIntegrationStatus.BLOCKED) {
			return ApprovalRequestLevel.BLOCKED;
		}
		if (presentationIntegration.status()
				== RecommendationPresentationIntegrationStatus.UNRELIABLE) {
			return ApprovalRequestLevel.UNRELIABLE;
		}
		if (presentationIntegration.status()
				== RecommendationPresentationIntegrationStatus.NOT_READY) {
			return ApprovalRequestLevel.NOT_READY;
		}
		if (presentationIntegration.status()
				== RecommendationPresentationIntegrationStatus.PARTIAL) {
			return ApprovalRequestLevel.PARTIAL;
		}
		if (requestable(
				presentationIntegration,
				operatorContext,
				humanApprovalRequired,
				approvalPolicy,
				lifecycleRisk,
				paymentSafetyUncertainty
		)) {
			return ApprovalRequestLevel.REQUESTABLE;
		}
		return ApprovalRequestLevel.UNKNOWN;
	}

	private ApprovalRequestReason reason(
			RecommendationPresentationIntegrationResult presentationIntegration,
			String operatorContext,
			boolean humanApprovalRequired,
			String approvalPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalRequestReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalRequestReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalRequestReason.MISSING_OPERATOR_CONTEXT;
		}
		if (!humanApprovalRequired) {
			return ApprovalRequestReason.MISSING_HUMAN_APPROVAL_REQUIREMENT;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalRequestReason.MISSING_APPROVAL_POLICY;
		}
		return switch (presentationIntegration.status()) {
			case EXPOSABLE -> ApprovalRequestReason.EXPOSABLE_PRESENTATION;
			case PARTIAL -> ApprovalRequestReason.PARTIAL_PRESENTATION;
			case NOT_READY -> ApprovalRequestReason.NOT_READY_PRESENTATION;
			case UNRELIABLE -> ApprovalRequestReason.UNRELIABLE_PRESENTATION;
			case BLOCKED -> ApprovalRequestReason.BLOCKED_PRESENTATION;
			case UNKNOWN -> ApprovalRequestReason.UNKNOWN;
		};
	}

	private ApprovalRequestScope scope(
			RecommendationPresentationIntegrationResult presentationIntegration,
			String operatorContext,
			boolean humanApprovalRequired,
			String approvalPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ApprovalRequestScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ApprovalRequestScope.LIFECYCLE_RISK;
		}
		if (missingOperatorContext(operatorContext)) {
			return ApprovalRequestScope.OPERATOR_CONTEXT;
		}
		if (!humanApprovalRequired) {
			return ApprovalRequestScope.HUMAN_APPROVAL;
		}
		if (missingApprovalPolicy(approvalPolicy)) {
			return ApprovalRequestScope.APPROVAL_POLICY;
		}
		if (presentationIntegration.status()
				== RecommendationPresentationIntegrationStatus.EXPOSABLE) {
			return ApprovalRequestScope.APPROVAL_REQUEST;
		}
		return ApprovalRequestScope.PRESENTATION;
	}

	private boolean requestable(
			RecommendationPresentationIntegrationResult presentationIntegration,
			String operatorContext,
			boolean humanApprovalRequired,
			String approvalPolicy,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		return presentationIntegration.status()
				== RecommendationPresentationIntegrationStatus.EXPOSABLE
				&& !missingOperatorContext(operatorContext)
				&& humanApprovalRequired
				&& !missingApprovalPolicy(approvalPolicy)
				&& !paymentSafetyUncertainty
				&& lifecycleRisk != OperationalUncertainty.CRITICAL;
	}

	private boolean missingOperatorContext(String operatorContext) {
		return operatorContext == null || operatorContext.isBlank();
	}

	private boolean missingApprovalPolicy(String approvalPolicy) {
		return approvalPolicy == null || approvalPolicy.isBlank();
	}
}
