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
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingDecision;
import com.fintech.sre.agent.runtime.reliability.ScenarioBindingStatus;
import com.fintech.sre.agent.runtime.reliability.ScenarioReference;
import com.fintech.sre.agent.runtime.reliability.VerificationReference;

class OperationalReliabilityRecommendationContentIntegrationTest {

	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String RUNBOOK_BINDING = "runbook/payment-latency-mitigation";
	private static final Instant GENERATED_AT = Instant.parse("2026-06-14T00:00:00Z");

	private final RecommendationCandidateEvaluator candidateEvaluator =
			new RecommendationCandidateEvaluator();
	private final RecommendationContentIntegration integration =
			new RecommendationContentIntegration();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.recommendationMutation()).isFalse();
		assertThat(integration.approvalRequest()).isFalse();
		assertThat(integration.actionCommand()).isFalse();
		assertThat(integration.executionPermission()).isFalse();
		assertThat(integration.operatorFacingContentExposureReadiness()).isTrue();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldMarkValidContentWithNoRiskAsContentReady() {
		RecommendationContentIntegrationResult result = integrate(validContent());

		assertThat(result.status())
				.isEqualTo(RecommendationContentIntegrationStatus.CONTENT_READY);
		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.VALID_CONTENT);
		assertThat(result.scope())
				.isEqualTo(RecommendationContentIntegrationScope.RECOMMENDATION_CONTENT);
		assertThat(result.operatorFacingContentVisible()).isTrue();
		assertThat(result.contentExposureCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldBlockWhenScenarioBindingMissing() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				false,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isIn(
						RecommendationContentIntegrationStatus.BLOCKED,
						RecommendationContentIntegrationStatus.NOT_READY
				);
		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.MISSING_SCENARIO_BINDING);
	}

	@Test
	void shouldBlockWhenRunbookBindingMissing() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				false,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isIn(
						RecommendationContentIntegrationStatus.BLOCKED,
						RecommendationContentIntegrationStatus.NOT_READY
				);
		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.MISSING_RUNBOOK_BINDING);
	}

	@Test
	void shouldBlockWhenRollbackBindingMissing() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				false,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isIn(
						RecommendationContentIntegrationStatus.BLOCKED,
						RecommendationContentIntegrationStatus.NOT_READY
				);
		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.MISSING_ROLLBACK_BINDING);
	}

	@Test
	void shouldBlockWhenVerificationBindingMissing() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				false,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isIn(
						RecommendationContentIntegrationStatus.BLOCKED,
						RecommendationContentIntegrationStatus.NOT_READY
				);
		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.MISSING_VERIFICATION_BINDING);
	}

	@Test
	void shouldBlockWhenPaymentSafetyClassificationMissing() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				false,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isEqualTo(RecommendationContentIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationContentIntegrationReason
						.MISSING_PAYMENT_SAFETY_CLASSIFICATION
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyClassificationIsInvalid() {
		RecommendationContent invalidContent = new RecommendationContentBuilder()
				.recommendationCandidate(eligibleCandidate())
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary("Use the matched runbook with rollback and verification references.")
				.recommendationType(RecommendationContentType.INCIDENT_RESPONSE)
				.reason(RecommendationContentReason.SCENARIO_MATCH)
				.scope(RecommendationContentScope.RECOMMENDATION_CONTENT)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.paymentSafetyClassification("UNKNOWN")
				.generatedAt(GENERATED_AT)
				.build();

		RecommendationContentIntegrationResult result = integrate(invalidContent);

		assertThat(result.status())
				.isEqualTo(RecommendationContentIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationContentIntegrationReason
						.INVALID_PAYMENT_SAFETY_CLASSIFICATION
		);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.CRITICAL,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isEqualTo(RecommendationContentIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationContentIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyIsUncertain() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				true,
				false,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isEqualTo(RecommendationContentIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(
				RecommendationContentIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
	}

	@Test
	void shouldProtectRawPayloadExposureAttempt() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				true,
				false,
				false,
				false
		);

		assertThat(result.status())
				.isEqualTo(RecommendationContentIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.RAW_PAYLOAD_PROTECTED);
		assertThat(result.scope())
				.isEqualTo(RecommendationContentIntegrationScope.PAYLOAD_PROTECTION);
	}

	@Test
	void shouldProtectVendorDetailExposureAttempt() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				true,
				false,
				false
		);

		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.VENDOR_DETAIL_PROTECTED);
	}

	@Test
	void shouldProtectCredentialExposureAttempt() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				true,
				false
		);

		assertThat(result.reason())
				.isEqualTo(RecommendationContentIntegrationReason.CREDENTIAL_PROTECTED);
	}

	@Test
	void shouldProtectConfigurationSecretExposureAttempt() {
		RecommendationContentIntegrationResult result = integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				true
		);

		assertThat(result.reason()).isEqualTo(
				RecommendationContentIntegrationReason.CONFIGURATION_SECRET_PROTECTED
		);
	}

	@Test
	void shouldRemainNonAuthoritative() {
		RecommendationContentIntegrationResult result = integrate(validContent());

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendationMutation()).isFalse();
		assertThat(result.approvalAuthority()).isFalse();
		assertThat(result.actionAuthority()).isFalse();
		assertThat(result.executionAuthority()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullInputs() {
		assertThatThrownBy(() -> integration.integrate(
				null,
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("content must not be null");

		assertThatThrownBy(() -> integration.integrate(
				validContent(),
				true,
				true,
				true,
				true,
				true,
				null,
				false,
				false,
				false,
				false,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleRisk must not be null");
	}

	private RecommendationContentIntegrationResult integrate(
			RecommendationContent content
	) {
		return integrate(
				content,
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false,
				false,
				false,
				false,
				false
		);
	}

	private RecommendationContentIntegrationResult integrate(
			RecommendationContent content,
			boolean scenarioBindingPresent,
			boolean runbookBindingPresent,
			boolean rollbackBindingPresent,
			boolean verificationBindingPresent,
			boolean paymentSafetyClassificationPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty,
			boolean rawPayloadExposureAttempt,
			boolean vendorDetailExposureAttempt,
			boolean credentialExposureAttempt,
			boolean configurationSecretExposureAttempt
	) {
		return integration.integrate(
				content,
				scenarioBindingPresent,
				runbookBindingPresent,
				rollbackBindingPresent,
				verificationBindingPresent,
				paymentSafetyClassificationPresent,
				lifecycleRisk,
				paymentSafetyUncertainty,
				rawPayloadExposureAttempt,
				vendorDetailExposureAttempt,
				credentialExposureAttempt,
				configurationSecretExposureAttempt
		);
	}

	private RecommendationContent validContent() {
		return new RecommendationContentBuilder()
				.recommendationCandidate(eligibleCandidate())
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary("Use the matched runbook with rollback and verification references.")
				.recommendationType(RecommendationContentType.INCIDENT_RESPONSE)
				.reason(RecommendationContentReason.SCENARIO_MATCH)
				.scope(RecommendationContentScope.RECOMMENDATION_CONTENT)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.paymentSafetyClassification("PAYMENT_SAFE_REVIEWED")
				.generatedAt(GENERATED_AT)
				.build();
	}

	private RecommendationCandidate eligibleCandidate() {
		return candidateEvaluator.evaluate(
				readyActionAdmissionReadiness(),
				RUNBOOK_BINDING
		);
	}

	private ActionAdmissionReadiness readyActionAdmissionReadiness() {
		return new ActionAdmissionReadiness(
				ActionAdmissionReadinessLevel.READY,
				ActionAdmissionReadinessReason.READY_VERIFICATION,
				ActionAdmissionReadinessScope.RUNTIME_READINESS,
				readyVerificationReadiness(),
				ACTION_TYPE,
				BLAST_RADIUS_BOUNDARY
		);
	}

	private VerificationReadiness readyVerificationReadiness() {
		return new VerificationReadiness(
				VerificationReadinessLevel.READY,
				VerificationReadinessReason.READY_APPROVAL,
				VerificationReadinessScope.RUNTIME_READINESS,
				readyApprovalReadiness(),
				true
		);
	}

	private ApprovalReadiness readyApprovalReadiness() {
		return new ApprovalReadiness(
				ApprovalReadinessLevel.READY,
				ApprovalReadinessReason.READY_RECOMMENDATION,
				ApprovalReadinessScope.RUNTIME_READINESS,
				readyRecommendationReadiness(),
				OPERATOR_CONTEXT
		);
	}

	private RecommendationReadiness readyRecommendationReadiness() {
		return new RecommendationReadiness(
				RecommendationReadinessLevel.READY,
				RecommendationReadinessReason.HIGH_RECOMMENDATION_RELIABILITY,
				RecommendationReadinessScope.RUNTIME_READINESS,
				highRecommendationReliability(),
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationReliability highRecommendationReliability() {
		return new RecommendationReliability(
				RecommendationReliabilityLevel.HIGH,
				RecommendationReliabilityReason.HIGH_DECISION_RELIABILITY,
				RecommendationReliabilityScope.DECISION,
				decisionReliability(),
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

	private DecisionReliability decisionReliability() {
		return new DecisionReliability(
				DecisionReliabilityLevel.HIGH,
				DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY,
				DecisionReliabilityScope.ASSESSMENT,
				assessmentReliability(),
				boundScenario(),
				boundRollbackVerification()
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
						Instant.parse("2026-06-14T00:00:00Z"),
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
}
