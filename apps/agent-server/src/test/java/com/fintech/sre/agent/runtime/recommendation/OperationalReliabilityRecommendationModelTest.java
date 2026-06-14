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

class OperationalReliabilityRecommendationModelTest {

	private static final String ACTION_TYPE = "restart-deployment";
	private static final String BLAST_RADIUS_BOUNDARY = "namespace/payments-prod";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String RUNBOOK_BINDING = "runbook/payment-latency-mitigation";
	private static final Instant GENERATED_AT = Instant.parse("2026-06-14T00:00:00Z");

	private final RecommendationCandidateEvaluator candidateEvaluator =
			new RecommendationCandidateEvaluator();
	private final RecommendationContentIntegration contentIntegration =
			new RecommendationContentIntegration();
	private final RecommendationGenerationEvaluator generationEvaluator =
			new RecommendationGenerationEvaluator();

	@Test
	void shouldCreateModelWhenGenerationIsGeneratableAndFieldsExist() {
		RecommendationModel model = standardBuilder(generatableGeneration()).build();

		assertThat(model.recommendationId()).isEqualTo("rec-001");
		assertThat(model.title()).isEqualTo("Mitigate payment latency degradation");
		assertThat(model.summary()).isEqualTo("Use the matched runbook with rollback and verification references.");
		assertThat(model.recommendationType())
				.isEqualTo(RecommendationModelType.INCIDENT_RESPONSE);
		assertThat(model.recommendationReason())
				.isEqualTo(RecommendationModelReason.SCENARIO_MATCH);
		assertThat(model.scenarioId()).isEqualTo("payments-degradation");
		assertThat(model.runbookId()).isEqualTo(RUNBOOK_BINDING);
		assertThat(model.rollbackId()).isEqualTo("rollback/payments");
		assertThat(model.verificationId()).isEqualTo("verification/payments");
		assertThat(model.evidenceReference()).isEqualTo("evidence/payment-latency-correlation");
		assertThat(model.paymentSafetyClassification()).isEqualTo("PAYMENT_SAFE_REVIEWED");
		assertThat(model.generatedAt()).isEqualTo(GENERATED_AT);
	}

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		RecommendationModel model = standardBuilder(generatableGeneration()).build();

		assertThat(model.readOnly()).isTrue();
		assertThat(model.llmOutput()).isFalse();
		assertThat(model.ragResult()).isFalse();
		assertThat(model.runbookSourceDocument()).isFalse();
		assertThat(model.approvalRequest()).isFalse();
		assertThat(model.actionCommand()).isFalse();
		assertThat(model.executionPermission()).isFalse();
		assertThat(model.exposesRawPayload()).isFalse();
		assertThat(model.exposesVendorDetail()).isFalse();
		assertThat(model.exposesCredential()).isFalse();
		assertThat(model.exposesConfigurationSecret()).isFalse();
	}

	@Test
	void shouldRejectModelCreationForPartialGeneration() {
		assertThatThrownBy(() -> standardBuilder(partialGeneration()).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("only GENERATABLE recommendation generation can create model");
	}

	@Test
	void shouldRejectModelCreationForNotReadyGeneration() {
		assertThatThrownBy(() -> standardBuilder(notReadyGeneration()).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("only GENERATABLE recommendation generation can create model");
	}

	@Test
	void shouldRejectModelCreationForUnreliableGeneration() {
		assertThatThrownBy(() -> standardBuilder(unreliableGeneration()).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("only GENERATABLE recommendation generation can create model");
	}

	@Test
	void shouldRejectModelCreationForBlockedGeneration() {
		assertThatThrownBy(() -> standardBuilder(blockedGeneration()).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("only GENERATABLE recommendation generation can create model");
	}

	@Test
	void shouldRejectMissingScenarioId() {
		assertThatThrownBy(() -> standardBuilder(generatableGeneration()).scenarioId(" ").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("scenarioId must not be blank");
	}

	@Test
	void shouldRejectMissingRunbookId() {
		assertThatThrownBy(() -> standardBuilder(generatableGeneration()).runbookId(" ").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("runbookId must not be blank");
	}

	@Test
	void shouldRejectMissingRollbackId() {
		assertThatThrownBy(() -> standardBuilder(generatableGeneration()).rollbackId(" ").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("rollbackId must not be blank");
	}

	@Test
	void shouldRejectMissingVerificationId() {
		assertThatThrownBy(() -> standardBuilder(generatableGeneration()).verificationId(" ").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("verificationId must not be blank");
	}

	@Test
	void shouldRejectMissingEvidenceReference() {
		assertThatThrownBy(() -> standardBuilder(generatableGeneration()).evidenceReference(" ").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("evidenceReference must not be blank");
	}

	@Test
	void shouldRejectMissingPaymentSafetyClassification() {
		assertThatThrownBy(() -> standardBuilder(generatableGeneration())
				.paymentSafetyClassification(" ")
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("paymentSafetyClassification must not be blank");
	}

	private RecommendationModelBuilder standardBuilder(
			RecommendationGeneration generation
	) {
		return new RecommendationModelBuilder()
				.recommendationGeneration(generation)
				.recommendationId("rec-001")
				.title("Mitigate payment latency degradation")
				.summary(
						"Use the matched runbook with rollback and verification references."
				)
				.recommendationType(RecommendationModelType.INCIDENT_RESPONSE)
				.recommendationReason(RecommendationModelReason.SCENARIO_MATCH)
				.scope(RecommendationModelScope.RECOMMENDATION_MODEL)
				.scenarioId("payments-degradation")
				.runbookId(RUNBOOK_BINDING)
				.rollbackId("rollback/payments")
				.verificationId("verification/payments")
				.evidenceReference("evidence/payment-latency-correlation")
				.paymentSafetyClassification("PAYMENT_SAFE_REVIEWED")
				.generatedAt(GENERATED_AT);
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

	private RecommendationGeneration partialGeneration() {
		return new RecommendationGeneration(
				RecommendationGenerationLevel.PARTIAL,
				RecommendationGenerationReason.PARTIAL_CONTENT,
				RecommendationGenerationScope.RECOMMENDATION_CONTENT,
				contentReadyResult()
		);
	}

	private RecommendationGeneration notReadyGeneration() {
		return new RecommendationGeneration(
				RecommendationGenerationLevel.NOT_READY,
				RecommendationGenerationReason.NOT_READY_CONTENT,
				RecommendationGenerationScope.RECOMMENDATION_CONTENT,
				contentReadyResult()
		);
	}

	private RecommendationGeneration unreliableGeneration() {
		return new RecommendationGeneration(
				RecommendationGenerationLevel.UNRELIABLE,
				RecommendationGenerationReason.UNRELIABLE_CONTENT,
				RecommendationGenerationScope.RECOMMENDATION_CONTENT,
				contentReadyResult()
		);
	}

	private RecommendationGeneration blockedGeneration() {
		return new RecommendationGeneration(
				RecommendationGenerationLevel.BLOCKED,
				RecommendationGenerationReason.BLOCKED_CONTENT,
				RecommendationGenerationScope.RECOMMENDATION_CONTENT,
				contentReadyResult()
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
