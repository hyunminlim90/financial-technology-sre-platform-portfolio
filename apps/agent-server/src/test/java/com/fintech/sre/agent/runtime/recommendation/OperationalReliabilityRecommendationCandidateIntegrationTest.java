package com.fintech.sre.agent.runtime.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadiness;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessReason;
import com.fintech.sre.agent.runtime.readiness.ActionAdmissionReadinessScope;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadiness;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadinessReason;
import com.fintech.sre.agent.runtime.readiness.ApprovalReadinessScope;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadiness;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadinessReason;
import com.fintech.sre.agent.runtime.readiness.RecommendationReadinessScope;
import com.fintech.sre.agent.runtime.readiness.VerificationReadiness;
import com.fintech.sre.agent.runtime.readiness.VerificationReadinessLevel;
import com.fintech.sre.agent.runtime.readiness.VerificationReadinessReason;
import com.fintech.sre.agent.runtime.readiness.VerificationReadinessScope;
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
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingRejectionReason;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioReference;
import com.fintech.sre.agent.runtime.reliability.VerificationReference;

class OperationalReliabilityRecommendationCandidateIntegrationTest {

	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String RUNBOOK_BINDING = "runbook/payment-latency-mitigation";

	private final RecommendationCandidateEvaluator evaluator =
			new RecommendationCandidateEvaluator();
	private final RecommendationCandidateIntegration integration =
			new RecommendationCandidateIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.recommendationGeneration()).isFalse();
		assertThat(integration.recommendationMutation()).isFalse();
		assertThat(integration.runbookSelection()).isFalse();
		assertThat(integration.llmOrRagInvocation()).isFalse();
		assertThat(integration.approvalRequest()).isFalse();
		assertThat(integration.actionCommand()).isFalse();
		assertThat(integration.executionPermission()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldAllowOnlyEligibleCandidateAsGenerationReadyViewCandidate() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				eligibleCandidate()
		);

		assertThat(result.status()).isEqualTo(
				RecommendationCandidateIntegrationStatus.GENERATION_READY
		);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.ELIGIBLE_CANDIDATE
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationCandidateIntegrationScope.RECOMMENDATION_CANDIDATE
		);
		assertThat(result.recommendationGenerationReadyView()).isTrue();
		assertThat(result.recommendationCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldBlockRecommendationGenerationForBlockedCandidate() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.BLOCKED_ACTION_ADMISSION)
		);

		assertThat(result.status()).isEqualTo(RecommendationCandidateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.BLOCKED_CANDIDATE
		);
		assertThat(result.recommendationGenerationReadyView()).isFalse();
	}

	@Test
	void shouldPreventRecommendationCertaintyForUnreliableCandidate() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				unreliableCandidate()
		);

		assertThat(result.status()).isEqualTo(
				RecommendationCandidateIntegrationStatus.UNRELIABLE
		);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.UNRELIABLE_CANDIDATE
		);
		assertThat(result.recommendationCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldRequireOperatorFacingWarningForNotReadyCandidate() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				notReadyCandidate()
		);

		assertThat(result.status()).isEqualTo(RecommendationCandidateIntegrationStatus.NOT_READY);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.NOT_READY_CANDIDATE
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationCandidateIntegrationScope.OPERATOR_VIEW
		);
	}

	@Test
	void shouldMarkPartialCandidateAsPartialRecommendationCandidate() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				partialCandidate()
		);

		assertThat(result.status()).isEqualTo(
				RecommendationCandidateIntegrationStatus.PARTIAL_CANDIDATE
		);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.PARTIAL_CANDIDATE
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationCandidateIntegrationScope.OPERATOR_VIEW
		);
	}

	@Test
	void shouldPropagateMissingScenarioBindingToLifecycleUncertainty() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.MISSING_SCENARIO_BINDING)
		);

		assertThat(result.status()).isEqualTo(RecommendationCandidateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.MISSING_SCENARIO_BINDING
		);
		assertThat(result.scope()).isEqualTo(RecommendationCandidateIntegrationScope.SCENARIO);
	}

	@Test
	void shouldPropagateMissingRunbookBindingToLifecycleUncertainty() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.MISSING_RUNBOOK_BINDING)
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.MISSING_RUNBOOK_BINDING
		);
		assertThat(result.scope()).isEqualTo(RecommendationCandidateIntegrationScope.RUNBOOK);
	}

	@Test
	void shouldPropagateMissingRollbackBindingToLifecycleUncertainty() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.MISSING_ROLLBACK_BINDING)
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.MISSING_ROLLBACK_BINDING
		);
		assertThat(result.scope()).isEqualTo(RecommendationCandidateIntegrationScope.ROLLBACK);
	}

	@Test
	void shouldPropagateMissingVerificationBindingToLifecycleUncertainty() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.MISSING_VERIFICATION_BINDING)
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.MISSING_VERIFICATION_BINDING
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationCandidateIntegrationScope.VERIFICATION
		);
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyAsCriticalLifecycleRisk() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.PAYMENT_SAFETY_UNCERTAINTY)
		);

		assertThat(result.status()).isEqualTo(RecommendationCandidateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationCandidateIntegrationScope.PAYMENT_SAFETY
		);
	}

	@Test
	void shouldPropagateCriticalLifecycleRiskAsGenerationBlocked() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				blockedCandidate(RecommendationCandidateReason.CRITICAL_LIFECYCLE_RISK)
		);

		assertThat(result.status()).isEqualTo(RecommendationCandidateIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationCandidateIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(
				RecommendationCandidateIntegrationScope.LIFECYCLE_RISK
		);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		RecommendationCandidateIntegrationResult result = integration.integrate(
				eligibleCandidate()
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.recommendationMutation()).isFalse();
		assertThat(result.approval()).isFalse();
		assertThat(result.actionCommand()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullRecommendationCandidate() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("recommendationCandidate must not be null");
	}

	private RecommendationCandidate eligibleCandidate() {
		return evaluator.evaluate(
				readyActionAdmissionReadiness(boundScenario(), boundRollbackVerification()),
				RUNBOOK_BINDING
		);
	}

	private RecommendationCandidate partialCandidate() {
		return evaluator.evaluate(partialActionAdmissionReadiness(), RUNBOOK_BINDING);
	}

	private RecommendationCandidate notReadyCandidate() {
		return evaluator.evaluate(notReadyActionAdmissionReadiness(), RUNBOOK_BINDING);
	}

	private RecommendationCandidate unreliableCandidate() {
		return evaluator.evaluate(unreliableActionAdmissionReadiness(), RUNBOOK_BINDING);
	}

	private RecommendationCandidate blockedCandidate(
			RecommendationCandidateReason forcedReason
	) {
		return switch (forcedReason) {
			case MISSING_SCENARIO_BINDING -> evaluator.evaluate(
					readyActionAdmissionReadiness(
							missingScenario(),
							boundRollbackVerification()
					),
					RUNBOOK_BINDING
			);
			case MISSING_RUNBOOK_BINDING -> evaluator.evaluate(
					readyActionAdmissionReadiness(
							boundScenario(),
							boundRollbackVerification()
					),
					" "
			);
			case MISSING_ROLLBACK_BINDING -> evaluator.evaluate(
					readyActionAdmissionReadiness(
							boundScenario(),
							missingRollbackBinding()
					),
					RUNBOOK_BINDING
			);
			case MISSING_VERIFICATION_BINDING -> evaluator.evaluate(
					readyActionAdmissionReadiness(
							boundScenario(),
							missingVerificationBinding()
					),
					RUNBOOK_BINDING
			);
			case PAYMENT_SAFETY_UNCERTAINTY -> evaluator.evaluate(
					readyActionAdmissionReadinessWithPaymentUncertainty(),
					RUNBOOK_BINDING
			);
			case CRITICAL_LIFECYCLE_RISK -> evaluator.evaluate(
					readyActionAdmissionReadinessWithCriticalRisk(),
					RUNBOOK_BINDING
			);
			case BLOCKED_ACTION_ADMISSION -> evaluator.evaluate(
					blockedActionAdmissionReadiness(),
					RUNBOOK_BINDING
			);
			default -> throw new IllegalArgumentException("unsupported forcedReason");
		};
	}

	private ActionAdmissionReadiness readyActionAdmissionReadiness(
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.READY,
				ActionAdmissionReadinessReason.READY_VERIFICATION,
				ActionAdmissionReadinessScope.RUNTIME_READINESS,
				readyVerificationReadiness(scenarioBindingDecision, bindingDecision),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReadiness readyActionAdmissionReadinessWithPaymentUncertainty() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.READY,
				ActionAdmissionReadinessReason.READY_VERIFICATION,
				ActionAdmissionReadinessScope.RUNTIME_READINESS,
				verificationReadinessWithPaymentUncertainty(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReadiness readyActionAdmissionReadinessWithCriticalRisk() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.READY,
				ActionAdmissionReadinessReason.READY_VERIFICATION,
				ActionAdmissionReadinessScope.RUNTIME_READINESS,
				verificationReadinessWithCriticalRisk(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReadiness partialActionAdmissionReadiness() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.PARTIAL,
				ActionAdmissionReadinessReason.PARTIAL_VERIFICATION,
				ActionAdmissionReadinessScope.OPERATOR_VIEW,
				readyVerificationReadiness(boundScenario(), boundRollbackVerification()),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReadiness notReadyActionAdmissionReadiness() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.NOT_READY,
				ActionAdmissionReadinessReason.NOT_READY_VERIFICATION,
				ActionAdmissionReadinessScope.OPERATOR_VIEW,
				readyVerificationReadiness(boundScenario(), boundRollbackVerification()),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReadiness unreliableActionAdmissionReadiness() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.UNRELIABLE,
				ActionAdmissionReadinessReason.UNRELIABLE_VERIFICATION,
				ActionAdmissionReadinessScope.VERIFICATION_READINESS,
				readyVerificationReadiness(boundScenario(), boundRollbackVerification()),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private ActionAdmissionReadiness blockedActionAdmissionReadiness() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.BLOCKED,
				ActionAdmissionReadinessReason.BLOCKED_VERIFICATION,
				ActionAdmissionReadinessScope.VERIFICATION_READINESS,
				readyVerificationReadiness(boundScenario(), boundRollbackVerification()),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private VerificationReadiness readyVerificationReadiness(
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadiness(scenarioBindingDecision, bindingDecision),
				true
		);
	}

	private VerificationReadiness verificationReadinessWithPaymentUncertainty() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				approvalReadinessWithPaymentUncertainty(),
				true
		);
	}

	private VerificationReadiness verificationReadinessWithCriticalRisk() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				approvalReadinessWithCriticalRisk(),
				true
		);
	}

	private ApprovalReadiness readyApprovalReadiness(
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(scenarioBindingDecision, bindingDecision),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness approvalReadinessWithPaymentUncertainty() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY,
						RecommendationReadinessScope.PAYMENT_SAFETY,
						highRecommendationReliability(
								boundScenario(),
								boundRollbackVerification()
						),
						OperationalUncertainty.LOW,
						false
				),
				OPERATOR_CONTEXT
		);
	}

	private ApprovalReadiness approvalReadinessWithCriticalRisk() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				new RecommendationReadiness(
						RecommendationReadinessLevel.READY,
						RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
						RecommendationReadinessScope.RUNTIME_READINESS,
						highRecommendationReliability(
								boundScenario(),
								boundRollbackVerification()
						),
						OperationalUncertainty.CRITICAL,
						false
				),
				OPERATOR_CONTEXT
		);
	}

	private RecommendationReadiness readyRecommendationReadiness(
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(scenarioBindingDecision, bindingDecision),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReliability highRecommendationReliability(
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				RecommendationReliabilityScope.DECISION,
				decisionReliability(scenarioBindingDecision, bindingDecision),
				requiredHumanApproval()
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

	private DecisionReliability decisionReliability(
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision bindingDecision
	) {
		return new DecisionReliability(
				DecisionReliabilityLevel.HIGH,
				DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.ASSESSMENT,
				assessmentReliability(),
				scenarioBindingDecision,
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

	private ScenarioBindingDecision missingScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.REJECTED,
				null,
				ScenarioBindingRejectionReason.MISSING_SCENARIO_REFERENCE
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
