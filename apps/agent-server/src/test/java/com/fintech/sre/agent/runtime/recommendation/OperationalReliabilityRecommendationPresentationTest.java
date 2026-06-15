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

class OperationalReliabilityRecommendationPresentationTest {

	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String RUNBOOK_BINDING = "runbook/payment-latency-mitigation";
	private static final Instant GENERATED_AT = Instant.parse("2026-06-14T00:00:00Z");
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-14T00:10:00Z");

	private final RecommendationCandidateEvaluator candidateEvaluator =
			new RecommendationCandidateEvaluator();
	private final RecommendationContentIntegration contentIntegration =
			new RecommendationContentIntegration();
	private final RecommendationGenerationEvaluator generationEvaluator =
			new RecommendationGenerationEvaluator();
	private final RecommendationModelIntegration modelIntegration =
			new RecommendationModelIntegration();

	@Test
	void shouldBePresentableWhenRecommendationReady() {
		RecommendationPresentation presentation = standardBuilder().build();

		assertThat(presentation.status())
				.isEqualTo(RecommendationPresentationStatus.PRESENTABLE);
		assertThat(presentation.reason())
				.isEqualTo(RecommendationPresentationReason.VALID_RECOMMENDATION);
		assertThat(presentation.scope())
				.isEqualTo(RecommendationPresentationScope.PRESENTATION);
	}

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		RecommendationPresentation presentation = standardBuilder().build();

		assertThat(presentation.readOnly()).isTrue();
		assertThat(presentation.recommendationMutation()).isFalse();
		assertThat(presentation.approvalRequest()).isFalse();
		assertThat(presentation.actionCommand()).isFalse();
		assertThat(presentation.executionPermission()).isFalse();
	}

	@Test
	void shouldBlockWhenScenarioReferenceMissing() {
		assertThatThrownBy(() -> standardBuilder().scenarioReference(" ").build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: MISSING_SCENARIO_REFERENCE");
	}

	@Test
	void shouldBlockWhenRunbookReferenceMissing() {
		assertThatThrownBy(() -> standardBuilder().runbookReference(" ").build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: MISSING_RUNBOOK_REFERENCE");
	}

	@Test
	void shouldBlockWhenRollbackReferenceMissing() {
		assertThatThrownBy(() -> standardBuilder().rollbackReference(" ").build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: MISSING_ROLLBACK_REFERENCE");
	}

	@Test
	void shouldBlockWhenVerificationReferenceMissing() {
		assertThatThrownBy(() -> standardBuilder().verificationReference(" ").build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: MISSING_VERIFICATION_REFERENCE");
	}

	@Test
	void shouldBlockWhenEvidenceReferenceMissing() {
		assertThatThrownBy(() -> standardBuilder().evidenceReference(" ").build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: MISSING_EVIDENCE_REFERENCE");
	}

	@Test
	void shouldBlockWhenPaymentSafetyClassificationMissing() {
		assertThatThrownBy(() -> standardBuilder()
				.paymentSafetyClassification(" ")
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage(
						"presentation blocked: MISSING_PAYMENT_SAFETY_CLASSIFICATION"
				);
	}

	@Test
	void shouldBlockWhenPaymentSafetyClassificationInvalid() {
		assertThatThrownBy(() -> standardBuilder()
				.paymentSafetyClassification("UNKNOWN")
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage(
						"presentation blocked: INVALID_PAYMENT_SAFETY_CLASSIFICATION"
				);
	}

	@Test
	void shouldBlockWhenPaymentSafetyIsUncertain() {
		assertThatThrownBy(() -> standardBuilder()
				.paymentSafetyUncertainty(true)
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: PAYMENT_SAFETY_UNCERTAINTY");
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		assertThatThrownBy(() -> standardBuilder()
				.lifecycleRisk(OperationalUncertainty.CRITICAL)
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: CRITICAL_LIFECYCLE_RISK");
	}

	@Test
	void shouldBlockWhenRawPayloadExposureAttemptExists() {
		assertThatThrownBy(() -> standardBuilder()
				.rawPayloadExposureAttempt(true)
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: RAW_PAYLOAD_PROTECTED");
	}

	@Test
	void shouldBlockWhenVendorDetailExposureAttemptExists() {
		assertThatThrownBy(() -> standardBuilder()
				.vendorDetailExposureAttempt(true)
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: VENDOR_DETAIL_PROTECTED");
	}

	@Test
	void shouldBlockWhenCredentialExposureAttemptExists() {
		assertThatThrownBy(() -> standardBuilder()
				.credentialExposureAttempt(true)
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: CREDENTIAL_PROTECTED");
	}

	@Test
	void shouldBlockWhenConfigurationSecretExposureAttemptExists() {
		assertThatThrownBy(() -> standardBuilder()
				.configurationSecretExposureAttempt(true)
				.build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("presentation blocked: CONFIGURATION_SECRET_PROTECTED");
	}

	private RecommendationPresentationBuilder standardBuilder() {
		return new RecommendationPresentationBuilder()
				.modelIntegrationResult(recommendationReadyResult())
				.scenarioReference("scenario/payments-degradation")
				.runbookReference(RUNBOOK_BINDING)
				.rollbackReference("rollback/payments")
				.verificationReference("verification/payments")
				.evidenceReference("evidence/payment-latency-correlation")
				.paymentSafetyClassification("PAYMENT_SAFE_REVIEWED")
				.presentationTimestamp(PRESENTED_AT)
				.lifecycleRisk(OperationalUncertainty.LOW)
				.paymentSafetyUncertainty(false);
	}

	private RecommendationModelIntegrationResult recommendationReadyResult() {
		return modelIntegration.integrate(
				validModel(),
				true,
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

	private RecommendationModel validModel() {
		return new RecommendationModelBuilder()
				.recommendationGeneration(generatableGeneration())
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary("Use the matched runbook with rollback and verification references.")
				.recommendationType(RecommendationModelType.INCIDENT_RESPONSE)
				.recommendationReason(RecommendationModelReason.SCENARIO_MATCH)
				.scope(RecommendationModelScope.RECOMMENDATION_MODEL)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.evidenceReference("evidence/payment-latency-correlation")
				.paymentSafetyClassification("PAYMENT_SAFE_REVIEWED")
				.generatedAt(GENERATED_AT)
				.build();
	}

	private RecommendationGeneration generatableGeneration() {
		return generationEvaluator.evaluate(
				contentReadyResult(),
				true,
				true,
				true,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationContentIntegrationResult contentReadyResult() {
		return contentIntegration.integrate(
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
				false
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

	private EvidenceGovernanceIntegrationResult governanceResult(EvidenceGovernancePolicy governancePolicy) {
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
				new RollbackReference("rollback/payments", "rollback-knowledge", true, false),
				new VerificationReference("verification/payments", "verification-knowledge", true, false, false),
				null
		);
	}
}
