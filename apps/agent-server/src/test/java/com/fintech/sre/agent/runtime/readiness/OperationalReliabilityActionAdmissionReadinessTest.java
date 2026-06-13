package com.fintech.sre.agent.runtime.readiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.runtime.reliability.AssessmentReliability;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.AssessmentReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.DecisionReliability;
import com.fintech.sre.agent.runtime.reliability.DecisionReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.DecisionReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.DecisionReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceClassification;
import com.fintech.sre.agent.runtime.reliability.EvidenceCompleteness;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidence;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidenceLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidenceReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceConfidenceScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernanceIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceGovernancePolicy;
import com.fintech.sre.agent.runtime.reliability.EvidenceIntegrityStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineage;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationResult;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceLineageIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceProvenance;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliability;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeApiRejectionReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeApiResponse;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeApiStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeSummaryReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeSummaryStatus;
import com.fintech.sre.agent.runtime.reliability.EvidenceRuntimeSummaryView;
import com.fintech.sre.agent.runtime.reliability.EvidenceSourceType;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScore;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScoreLevel;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScoreReason;
import com.fintech.sre.agent.runtime.reliability.EvidenceTrustScoreScope;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalDecision;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalReason;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalRequirement;
import com.fintech.sre.agent.runtime.reliability.HumanApprovalScope;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliability;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityLevel;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityReason;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliabilityScope;
import com.fintech.sre.agent.runtime.reliability.RollbackReference;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingDecision;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingRejectionReason;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingDecision;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioReference;
import com.fintech.sre.agent.runtime.reliability.VerificationReference;

class OperationalReliabilityActionAdmissionReadinessTest {

	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";

	private final ActionAdmissionReadinessEvaluator evaluator =
			new ActionAdmissionReadinessEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.readOnly()).isTrue();
		assertThat(readiness.actualActionCommandGeneration()).isFalse();
		assertThat(readiness.actualActionAdmissionResult()).isFalse();
		assertThat(readiness.executionPermission()).isFalse();
		assertThat(readiness.approvalGeneration()).isFalse();
	}

	@Test
	void shouldBeReadyWhenVerificationReadinessIsReadyAndPrerequisitesExist() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.READY);
		assertThat(readiness.reason()).isEqualTo(ActionAdmissionReadinessReason.READY_VERIFICATION);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.RUNTIME_READINESS);
	}

	@Test
	void shouldBlockWhenVerificationReadinessIsBlocked() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				blockedVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.BLOCKED_VERIFICATION);
	}

	@Test
	void shouldBeUnreliableWhenVerificationReadinessIsUnreliable() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				unreliableVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.UNRELIABLE);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.UNRELIABLE_VERIFICATION);
	}

	@Test
	void shouldBeNotReadyWhenVerificationReadinessIsNotReady() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				notReadyVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.NOT_READY);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.NOT_READY_VERIFICATION);
	}

	@Test
	void shouldBePartialWhenVerificationReadinessIsPartial() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				partialVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.PARTIAL_VERIFICATION);
	}

	@Test
	void shouldBlockWhenActionTypeMissing() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadiness(),
				" ",
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.MISSING_ACTION_TYPE);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.ACTION_TYPE);
	}

	@Test
	void shouldBlockWhenBlastRadiusBoundaryMissing() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadiness(),
				ACTION_TYPE,
				" "
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.MISSING_BLAST_RADIUS_BOUNDARY);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.BLAST_RADIUS);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadinessWithMissingRollbackBinding(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.MISSING_ROLLBACK_BINDING);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.ROLLBACK_BOUNDARY);
	}

	@Test
	void shouldBlockWhenVerificationBindingMissing() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadinessWithMissingVerificationBinding(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.MISSING_VERIFICATION_BINDING);
		assertThat(readiness.scope())
				.isEqualTo(ActionAdmissionReadinessScope.VERIFICATION_BOUNDARY);
	}

	@Test
	void shouldBlockWhenHumanApprovalRequirementMissing() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadinessWithoutHumanApprovalRequirement(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason()).isEqualTo(
				ActionAdmissionReadinessReason.MISSING_HUMAN_APPROVAL_REQUIREMENT
		);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.HUMAN_APPROVAL);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadinessWithCriticalRisk(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				paymentVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.BLOCKED);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(ActionAdmissionReadinessScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBePartialWhenLifecycleUncertaintyExists() {
		ActionAdmissionReadiness readiness = evaluator.evaluate(
				readyVerificationReadinessWithUncertainty(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);

		assertThat(readiness.level()).isEqualTo(ActionAdmissionReadinessLevel.PARTIAL);
		assertThat(readiness.reason())
				.isEqualTo(ActionAdmissionReadinessReason.LIFECYCLE_UNCERTAINTY);
		assertThat(readiness.scope()).isEqualTo(
				ActionAdmissionReadinessScope.LIFECYCLE_UNCERTAINTY
		);
	}

	@Test
	void shouldRejectNullVerificationReadiness() {
		assertThatThrownBy(() -> evaluator.evaluate(null, ACTION_TYPE, BLAST_RADIUS_BOUNDARY))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("verificationReadiness must not be null");
	}

	private VerificationReadiness readyVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadiness(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness readyVerificationReadinessWithCriticalRisk() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadinessWithCriticalRisk(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness readyVerificationReadinessWithUncertainty() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadinessWithUncertainty(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness readyVerificationReadinessWithMissingVerificationBinding() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadiness(missingVerificationBinding()),
				true
		);
	}

	private VerificationReadiness readyVerificationReadinessWithMissingRollbackBinding() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadiness(missingRollbackBinding()),
				true
		);
	}

	private VerificationReadiness readyVerificationReadinessWithoutHumanApprovalRequirement() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadinessWithoutHumanApprovalRequirement(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness partialVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.PARTIAL,
				VerificationReadinessReason.PARTIAL_APPROVAL,
				VerificationReadinessScope.OPERATOR_VIEW,
				partialApprovalReadiness(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness notReadyVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.NOT_READY,
				VerificationReadinessReason.NOT_READY_APPROVAL,
				VerificationReadinessScope.OPERATOR_VIEW,
				notReadyApprovalReadiness(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness unreliableVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.UNRELIABLE,
				VerificationReadinessReason.UNRELIABLE_APPROVAL,
				VerificationReadinessScope.APPROVAL_READINESS,
				unreliableApprovalReadiness(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness blockedVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.BLOCKED,
				VerificationReadinessReason.BLOCKED_APPROVAL,
				VerificationReadinessScope.APPROVAL_READINESS,
				blockedApprovalReadiness(boundRollbackVerification()),
				true
		);
	}

	private VerificationReadiness paymentVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.BLOCKED,
				VerificationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
				VerificationReadinessScope.PAYMENT_SAFETY,
				paymentApprovalReadiness(boundRollbackVerification()),
				true
		);
	}

	private ApprovalReadiness readyApprovalReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithCriticalRisk(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
						RecommendationReadinessScope.RUNTIME_READINESS,
						highRecommendationReliability(bindingDecision, requiredHumanApproval()),
						OperationalUncertainty.CRITICAL,
						false
				),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithUncertainty(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
						RecommendationReadinessScope.RUNTIME_READINESS,
						highRecommendationReliability(bindingDecision, requiredHumanApproval()),
						OperationalUncertainty.LOW,
						true
				),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness readyApprovalReadinessWithoutHumanApprovalRequirement(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
						RecommendationReadinessScope.RUNTIME_READINESS,
						highRecommendationReliability(bindingDecision, approvalNotRequired()),
						OperationalUncertainty.LOW,
						false
				),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness partialApprovalReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.PARTIAL,
				ApprovalReadinessReason.PARTIAL_RECOMMENDATION,
				ApprovalReadinessScope.OPERATOR_VIEW,
				partialRecommendationReadiness(bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness notReadyApprovalReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.NOT_READY,
				ApprovalReadinessReason.NOT_READY_RECOMMENDATION,
				ApprovalReadinessScope.OPERATOR_VIEW,
				notReadyRecommendationReadiness(bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness unreliableApprovalReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.UNRELIABLE,
				ApprovalReadinessReason.UNRELIABLE_RECOMMENDATION,
				ApprovalReadinessScope.RECOMMENDATION_READINESS,
				unreliableRecommendationReadiness(bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness blockedApprovalReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.BLOCKED,
				ApprovalReadinessReason.BLOCKED_RECOMMENDATION,
				ApprovalReadinessScope.RECOMMENDATION_READINESS,
				blockedRecommendationReadiness(bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness paymentApprovalReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.BLOCKED,
				ApprovalReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
				ApprovalReadinessScope.PAYMENT_SAFETY,
				paymentRecommendationReadiness(bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private RecommendationReadiness readyRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(bindingDecision, requiredHumanApproval()),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness partialRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.PARTIAL,
				RecommendationReadinessReason.MEDIUM_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.OPERATOR_VIEW,
				mediumRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness notReadyRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.NOT_READY,
				RecommendationReadinessReason.LOW_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.OPERATOR_VIEW,
				lowRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness unreliableRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.UNRELIABLE,
				RecommendationReadinessReason.UNRELIABLE_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RECOMMENDATION_RELIABILITY,
				unreliableRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness blockedRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.BLOCKED,
				RecommendationReadinessReason.BLOCKED_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RECOMMENDATION_RELIABILITY,
				blockedRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReadiness paymentRecommendationReadiness(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.BLOCKED,
				RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
				RecommendationReadinessScope.PAYMENT_SAFETY,
				paymentRecommendationReliability(bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReliability highRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision,
			HumanApprovalDecision humanApprovalDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				bindingDecision,
				humanApprovalDecision
		);
	}

	private RecommendationReliability mediumRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.MEDIUM,
				RecommendationReliabilityReason.UNKNOWN,
				bindingDecision,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability lowRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.LOW,
				RecommendationReliabilityReason.LOW_DECISION_RELIABILITY,
				bindingDecision,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability unreliableRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.UNRELIABLE,
				RecommendationReliabilityReason.UNRELIABLE_DECISION,
				bindingDecision,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability blockedRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.BLOCKED,
				RecommendationReliabilityReason.BLOCKED_DECISION,
				bindingDecision,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability paymentRecommendationReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return recommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				bindingDecision,
				requiredHumanApproval()
		);
	}

	private RecommendationReliability recommendationReliability(
			RecommendationReliabilityLevel level,
			RecommendationReliabilityReason reason,
			RollbackVerificationBindingDecision bindingDecision,
			HumanApprovalDecision humanApprovalDecision
	) {
		return new RecommendationReliability(
				level,
				reason,
				level == RecommendationReliabilityLevel.MEDIUM
						? RecommendationReliabilityScope.OPERATOR_VIEW
						: reason == RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
						? RecommendationReliabilityScope.PAYMENT_SAFETY
						: RecommendationReliabilityScope.DECISION,
				decisionReliability(bindingDecision),
				humanApprovalDecision
		);
	}

	private HumanApprovalDecision requiredHumanApproval() {
		return new HumanApprovalDecision(
				true,
				HumanApprovalScope.REQUIRED,
				new HumanApprovalRequirement(true, true, false, false),
				List.of(HumanApprovalReason.HIGH_RISK_REQUIRES_HUMAN_APPROVAL)
		);
	}

	private HumanApprovalDecision approvalNotRequired() {
		return new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of()
		);
	}

	private DecisionReliability decisionReliability(
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new DecisionReliability(
				DecisionReliabilityLevel.HIGH,
				DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.ASSESSMENT,
				assessmentReliability(),
				boundScenario(),
				bindingDecision
		);
	}

	private AssessmentReliability assessmentReliability() {
		return new AssessmentReliability(
				AssessmentReliabilityLevel.HIGH,
				AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
				AssessmentReliabilityScope.ASSESSMENT,
				evidenceReliability(),
				true
		);
	}

	private EvidenceReliability evidenceReliability() {
		EvidenceGovernancePolicy governancePolicy = new EvidenceGovernancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.PUBLIC_SAFE,
				new EvidenceProvenance(
						EvidenceSourceType.METRICS,
						"adapter/prometheus",
						Instant.parse("2026-06-13T00:00:00Z"),
						true,
						false,
						false
				)
		);
		EvidenceLineage lineage = EvidenceLineage.trace(governancePolicy, true, true);
		EvidenceTrustScore trustScore = new EvidenceTrustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				EvidenceTrustScoreScope.EVIDENCE,
				governanceResult(governancePolicy),
				lineageResult(lineage)
		);
		EvidenceConfidence confidence = new EvidenceConfidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.SUFFICIENT_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore
		);
		return new EvidenceReliability(
				EvidenceReliabilityLevel.HIGH,
				EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
				EvidenceReliabilityScope.EVIDENCE,
				governancePolicy,
				lineage,
				trustScore,
				confidence,
				true,
				false
		);
	}

	private EvidenceGovernanceIntegrationResult governanceResult(
			EvidenceGovernancePolicy governancePolicy
	) {
		return new EvidenceGovernanceIntegrationResult(
				governancePolicy,
				readableApiResponse(),
				EvidenceGovernanceIntegrationStatus.INTEGRATED,
				EvidenceGovernanceIntegrationReason.UNKNOWN,
				EvidenceGovernanceIntegrationScope.API_EXPOSURE
		);
	}

	private EvidenceLineageIntegrationResult lineageResult(EvidenceLineage lineage) {
		return new EvidenceLineageIntegrationResult(
				lineage,
				readableApiResponse(),
				EvidenceLineageIntegrationStatus.INTEGRATED,
				EvidenceLineageIntegrationReason.UNKNOWN,
				EvidenceLineageIntegrationScope.API_EXPOSURE
		);
	}

	private EvidenceRuntimeApiResponse readableApiResponse() {
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.HEALTHY,
						OperationalUncertainty.LOW,
						OperationalUncertainty.LOW,
						false,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						true,
						EvidenceCompleteness.COMPLETE
				),
				EvidenceRuntimeApiStatus.READABLE,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private ScenarioBindingDecision boundScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				new ScenarioReference("payments-degradation", "scenario-knowledge", true, false),
				null
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				new RollbackReference(
						"rollback/payments",
						"rollback-knowledge",
						true,
						false
				),
				new VerificationReference(
						"verification/payments",
						"verification-knowledge",
						true,
						false,
						false
				),
				null
		);
	}

	private RollbackVerificationBindingDecision missingVerificationBinding() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.REJECTED,
				new RollbackReference(
						"rollback/payments",
						"rollback-knowledge",
						true,
						false
				),
				null,
				RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE
		);
	}

	private RollbackVerificationBindingDecision missingRollbackBinding() {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.REJECTED,
				null,
				new VerificationReference(
						"verification/payments",
						"verification-knowledge",
						true,
						false,
						false
				),
				RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE
		);
	}
}
