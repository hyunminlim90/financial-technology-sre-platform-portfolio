package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityDecisionReliabilityTest {

	private final DecisionReliabilityEvaluator evaluator =
			new DecisionReliabilityEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		DecisionReliability reliability = evaluator.evaluate(
				assessmentReliability(
						AssessmentReliabilityLevel.HIGH,
						AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
						true,
						evidenceReliability(
								EvidenceReliabilityLevel.HIGH,
								EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
								false,
								true,
								confidence(
										EvidenceConfidenceLevel.HIGH,
										EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
										EvidenceConfidenceScope.ASSESSMENT,
										trustScore(
												EvidenceTrustScoreLevel.HIGH,
												EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
												governancePolicy(
														EvidenceTrustLevel.TRUSTED,
														EvidenceIntegrityStatus.INTACT,
														EvidenceClassification.PUBLIC_SAFE,
														provenance(true, false, false)
												),
												lineage(
														EvidenceLineageStatus.COMPLETE,
														EvidenceLineageReason.UNKNOWN,
														governancePolicy(
																EvidenceTrustLevel.TRUSTED,
																EvidenceIntegrityStatus.INTACT,
																EvidenceClassification.PUBLIC_SAFE,
																provenance(true, false, false)
														)
												)
										)
								)
						)
				),
				boundScenario(),
				boundRollbackVerification(false)
		);

		assertThat(reliability.readOnly()).isTrue();
		assertThat(reliability.recommendation()).isFalse();
		assertThat(reliability.executionPermission()).isFalse();
		assertThat(reliability.actionAdmission()).isFalse();
		assertThat(reliability.actionDecision()).isFalse();
		assertThat(reliability.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBlockDecisionForBlockedAssessmentReliability() {
		DecisionReliability reliability = evaluator.evaluate(
				assessmentReliability(
						AssessmentReliabilityLevel.BLOCKED,
						AssessmentReliabilityReason.BLOCKED_EVIDENCE,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.BLOCKED,
								EvidenceReliabilityReason.GOVERNANCE_BLOCKED,
								false,
								false,
								confidence(
										EvidenceConfidenceLevel.LOW,
										EvidenceConfidenceReason.PARTIAL_EVIDENCE,
										EvidenceConfidenceScope.API_BOUNDARY,
										trustScore(
												EvidenceTrustScoreLevel.UNTRUSTED,
												EvidenceTrustScoreReason.BLOCKED_EVIDENCE,
												governancePolicy(
														EvidenceTrustLevel.UNTRUSTED,
														EvidenceIntegrityStatus.DEGRADED,
														EvidenceClassification.BLOCKED,
														provenance(true, false, true)
												),
												lineage(
														EvidenceLineageStatus.BLOCKED,
														EvidenceLineageReason.BLOCKED_EVIDENCE,
														governancePolicy(
																EvidenceTrustLevel.UNTRUSTED,
																EvidenceIntegrityStatus.DEGRADED,
																EvidenceClassification.BLOCKED,
																provenance(true, false, true)
														)
												)
										)
								)
						)
				),
				boundScenario(),
				boundRollbackVerification(false)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.BLOCKED_ASSESSMENT);
	}

	@Test
	void shouldMarkUnreliableAssessmentAsUnreliableDecision() {
		DecisionReliability reliability = evaluator.evaluate(
				assessmentReliability(
						AssessmentReliabilityLevel.UNRELIABLE,
						AssessmentReliabilityReason.UNRELIABLE_EVIDENCE,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.UNRELIABLE,
								EvidenceReliabilityReason.UNTRUSTED_EVIDENCE,
								false,
								false,
								confidence(
										EvidenceConfidenceLevel.MEDIUM,
										EvidenceConfidenceReason.PARTIAL_EVIDENCE,
										EvidenceConfidenceScope.ASSESSMENT,
										trustScore(
												EvidenceTrustScoreLevel.UNTRUSTED,
												EvidenceTrustScoreReason.MISSING_PROVENANCE,
												governancePolicy(
														EvidenceTrustLevel.UNTRUSTED,
														EvidenceIntegrityStatus.MISSING,
														EvidenceClassification.UNKNOWN,
														EvidenceProvenance.missingProvenance()
												),
												lineage(
														EvidenceLineageStatus.PARTIAL,
														EvidenceLineageReason.MISSING_PROVENANCE,
														governancePolicy(
																EvidenceTrustLevel.UNTRUSTED,
																EvidenceIntegrityStatus.MISSING,
																EvidenceClassification.UNKNOWN,
																EvidenceProvenance.missingProvenance()
														)
												)
										)
								)
						)
				),
				boundScenario(),
				boundRollbackVerification(false)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.UNRELIABLE);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.UNRELIABLE_ASSESSMENT);
	}

	@Test
	void shouldDowngradeLowAssessmentReliability() {
		DecisionReliability reliability = evaluator.evaluate(
				assessmentReliability(
						AssessmentReliabilityLevel.LOW,
						AssessmentReliabilityReason.LOW_EVIDENCE_RELIABILITY,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.LOW,
								EvidenceReliabilityReason.LOW_CONFIDENCE,
								false,
								true,
								confidence(
										EvidenceConfidenceLevel.LOW,
										EvidenceConfidenceReason.PARTIAL_EVIDENCE,
										EvidenceConfidenceScope.OPERATOR_VIEW,
										trustScore(
												EvidenceTrustScoreLevel.LOW,
												EvidenceTrustScoreReason.DEGRADED_INTEGRITY,
												governancePolicy(
														EvidenceTrustLevel.TRUSTED,
														EvidenceIntegrityStatus.DEGRADED,
														EvidenceClassification.PUBLIC_SAFE,
														provenance(true, false, false)
												),
												lineage(
														EvidenceLineageStatus.COMPLETE,
														EvidenceLineageReason.UNKNOWN,
														governancePolicy(
																EvidenceTrustLevel.TRUSTED,
																EvidenceIntegrityStatus.DEGRADED,
																EvidenceClassification.PUBLIC_SAFE,
																provenance(true, false, false)
														)
												)
										)
								)
						)
				),
				boundScenario(),
				boundRollbackVerification(false)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.LOW);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.LOW_ASSESSMENT_RELIABILITY);
	}

	@Test
	void shouldBlockDecisionWhenScenarioBindingMissing() {
		DecisionReliability reliability = evaluator.evaluate(
				highAssessmentReliability(),
				null,
				boundRollbackVerification(false)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.MISSING_SCENARIO_BINDING);
		assertThat(reliability.scope()).isEqualTo(DecisionReliabilityScope.SCENARIO_BOUNDARY);
	}

	@Test
	void shouldBlockDecisionWhenRollbackBindingMissing() {
		DecisionReliability reliability = evaluator.evaluate(
				highAssessmentReliability(),
				boundScenario(),
				new RollbackVerificationBindingDecision(
						RollbackVerificationBindingStatus.REJECTED,
						null,
						verificationReference(true, false, false),
						RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE
				)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.MISSING_ROLLBACK_BINDING);
		assertThat(reliability.scope()).isEqualTo(DecisionReliabilityScope.ROLLBACK_BOUNDARY);
	}

	@Test
	void shouldBlockDecisionWhenVerificationBindingMissing() {
		DecisionReliability reliability = evaluator.evaluate(
				highAssessmentReliability(),
				boundScenario(),
				new RollbackVerificationBindingDecision(
						RollbackVerificationBindingStatus.REJECTED,
						rollbackReference(false),
						null,
						RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE
				)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.MISSING_VERIFICATION_BINDING);
		assertThat(reliability.scope()).isEqualTo(DecisionReliabilityScope.VERIFICATION_BOUNDARY);
	}

	@Test
	void shouldDowngradeDecisionForPaymentSafetyUncertainty() {
		DecisionReliability reliability = evaluator.evaluate(
				assessmentReliability(
						AssessmentReliabilityLevel.LOW,
						AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.RESTRICTED,
								EvidenceReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
								true,
								false,
								confidence(
										EvidenceConfidenceLevel.INSUFFICIENT,
										EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING,
										EvidenceConfidenceScope.PAYMENT_EVIDENCE,
										trustScore(
												EvidenceTrustScoreLevel.MEDIUM,
												EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
												governancePolicy(
														EvidenceTrustLevel.TRUSTED,
														EvidenceIntegrityStatus.INTACT,
														EvidenceClassification.RESTRICTED,
														provenance(true, false, false)
												),
												lineage(
														EvidenceLineageStatus.RESTRICTED,
														EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE,
														governancePolicy(
																EvidenceTrustLevel.TRUSTED,
																EvidenceIntegrityStatus.INTACT,
																EvidenceClassification.RESTRICTED,
																provenance(true, false, false)
														)
												)
										)
								)
						)
				),
				boundScenario(),
				boundRollbackVerification(true)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.LOW);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(reliability.scope()).isEqualTo(DecisionReliabilityScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldDowngradeContradictoryAssessment() {
		DecisionReliability reliability = evaluator.evaluate(
				assessmentReliability(
						AssessmentReliabilityLevel.LOW,
						AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE,
						true,
						evidenceReliability(
								EvidenceReliabilityLevel.LOW,
								EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE,
								false,
								true,
								confidence(
										EvidenceConfidenceLevel.LOW,
										EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE,
										EvidenceConfidenceScope.OPERATOR_VIEW,
										trustScore(
												EvidenceTrustScoreLevel.LOW,
												EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE,
												governancePolicy(
														EvidenceTrustLevel.TRUSTED,
														EvidenceIntegrityStatus.CONTRADICTORY,
														EvidenceClassification.PUBLIC_SAFE,
														provenance(true, false, false)
												),
												lineage(
														EvidenceLineageStatus.PARTIAL,
														EvidenceLineageReason.CONTRADICTORY_EVIDENCE,
														governancePolicy(
																EvidenceTrustLevel.TRUSTED,
																EvidenceIntegrityStatus.CONTRADICTORY,
																EvidenceClassification.PUBLIC_SAFE,
																provenance(true, false, false)
														)
												)
										)
								)
						)
				),
				boundScenario(),
				boundRollbackVerification(false)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.LOW);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.CONTRADICTORY_ASSESSMENT);
		assertThat(reliability.scope()).isEqualTo(DecisionReliabilityScope.LIFECYCLE);
	}

	@Test
	void shouldRequireAllConditionsForHighDecisionReliability() {
		DecisionReliability reliability = evaluator.evaluate(
				highAssessmentReliability(),
				boundScenario(),
				boundRollbackVerification(false)
		);

		assertThat(reliability.level()).isEqualTo(DecisionReliabilityLevel.HIGH);
		assertThat(reliability.reason()).isEqualTo(DecisionReliabilityReason.HIGH_ASSESSMENT_RELIABILITY);
	}

	@Test
	void shouldRejectNullAssessmentReliability() {
		assertThatThrownBy(() -> evaluator.evaluate(null, boundScenario(), boundRollbackVerification(false)))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessmentReliability must not be null");
	}

	private AssessmentReliability highAssessmentReliability() {
		return assessmentReliability(
				AssessmentReliabilityLevel.HIGH,
				AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
				true,
				evidenceReliability(
						EvidenceReliabilityLevel.HIGH,
						EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
						false,
						true,
						confidence(
								EvidenceConfidenceLevel.HIGH,
								EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
								EvidenceConfidenceScope.ASSESSMENT,
								trustScore(
										EvidenceTrustScoreLevel.HIGH,
										EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
										governancePolicy(
												EvidenceTrustLevel.TRUSTED,
												EvidenceIntegrityStatus.INTACT,
												EvidenceClassification.PUBLIC_SAFE,
												provenance(true, false, false)
										),
										lineage(
												EvidenceLineageStatus.COMPLETE,
												EvidenceLineageReason.UNKNOWN,
												governancePolicy(
														EvidenceTrustLevel.TRUSTED,
														EvidenceIntegrityStatus.INTACT,
														EvidenceClassification.PUBLIC_SAFE,
														provenance(true, false, false)
												)
										)
								)
						)
				)
		);
	}

	private ScenarioBindingDecision boundScenario() {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				new ScenarioReference("scenario-1", "knowledge-1", true, false),
				null
		);
	}

	private RollbackVerificationBindingDecision boundRollbackVerification(
			boolean paymentSafety
	) {
		return new RollbackVerificationBindingDecision(
				RollbackVerificationBindingStatus.BOUND,
				rollbackReference(false),
				verificationReference(paymentSafety, false, true),
				null
		);
	}

	private RollbackReference rollbackReference(boolean deprecated) {
		return new RollbackReference("rollback-1", "knowledge-1", true, deprecated);
	}

	private VerificationReference verificationReference(
			boolean paymentConsistencyVerification,
			boolean deprecated,
			boolean known
	) {
		return new VerificationReference(
				"verification-1",
				"knowledge-1",
				known,
				deprecated,
				paymentConsistencyVerification
		);
	}

	private AssessmentReliability assessmentReliability(
			AssessmentReliabilityLevel level,
			AssessmentReliabilityReason reason,
			boolean assessmentCertaintyAllowed,
			EvidenceReliability evidenceReliability
	) {
		return new AssessmentReliability(
				level,
				reason,
				evidenceReliability.scope() == EvidenceReliabilityScope.PAYMENT_EVIDENCE
						? AssessmentReliabilityScope.PAYMENT_EVIDENCE
						: AssessmentReliabilityScope.ASSESSMENT,
				evidenceReliability,
				assessmentCertaintyAllowed
		);
	}

	private EvidenceReliability evidenceReliability(
			EvidenceReliabilityLevel level,
			EvidenceReliabilityReason reason,
			boolean paymentSafetyUncertainty,
			boolean assessmentCertaintyAllowed,
			EvidenceConfidence confidence
	) {
		return new EvidenceReliability(
				level,
				reason,
				confidence.scope() == EvidenceConfidenceScope.PAYMENT_EVIDENCE
						? EvidenceReliabilityScope.PAYMENT_EVIDENCE
						: EvidenceReliabilityScope.ASSESSMENT,
				confidence.trustScore().governanceIntegrationResult().governancePolicy(),
				confidence.trustScore().lineageIntegrationResult().lineage(),
				confidence.trustScore(),
				confidence,
				assessmentCertaintyAllowed,
				paymentSafetyUncertainty
		);
	}

	private EvidenceConfidence confidence(
			EvidenceConfidenceLevel level,
			EvidenceConfidenceReason reason,
			EvidenceConfidenceScope scope,
			EvidenceTrustScore trustScore
	) {
		return new EvidenceConfidence(level, reason, scope, trustScore);
	}

	private EvidenceTrustScore trustScore(
			EvidenceTrustScoreLevel level,
			EvidenceTrustScoreReason reason,
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage
	) {
		EvidenceRuntimeApiResponse apiResponse = apiResponse();
		return new EvidenceTrustScore(
				level,
				reason,
				governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceTrustScoreScope.PAYMENT_EVIDENCE
						: EvidenceTrustScoreScope.EVIDENCE,
				new EvidenceGovernanceIntegrationResult(
						governancePolicy,
						apiResponse,
						EvidenceGovernanceIntegrationStatus.INTEGRATED,
						EvidenceGovernanceIntegrationReason.UNKNOWN,
						EvidenceGovernanceIntegrationScope.API_EXPOSURE
				),
				new EvidenceLineageIntegrationResult(
						lineage,
						apiResponse,
						EvidenceLineageIntegrationStatus.INTEGRATED,
						EvidenceLineageIntegrationReason.UNKNOWN,
						EvidenceLineageIntegrationScope.API_EXPOSURE
				)
		);
	}

	private EvidenceLineage lineage(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy governancePolicy
	) {
		return new EvidenceLineage(
				List.of(
						EvidenceLineageNode.SOURCE,
						EvidenceLineageNode.ADAPTER,
						EvidenceLineageNode.ROUTING,
						EvidenceLineageNode.DISPATCH,
						EvidenceLineageNode.EXECUTION,
						EvidenceLineageNode.COLLECTION,
						EvidenceLineageNode.ASSESSMENT,
						EvidenceLineageNode.SUMMARY
				),
				List.of(
						new EvidenceLineageEdge(EvidenceLineageNode.SOURCE, EvidenceLineageNode.ADAPTER),
						new EvidenceLineageEdge(EvidenceLineageNode.ADAPTER, EvidenceLineageNode.ROUTING)
				),
				status,
				reason,
				governancePolicy,
				status == EvidenceLineageStatus.BLOCKED
						? OperationalUncertainty.CRITICAL
						: status == EvidenceLineageStatus.RESTRICTED
						? OperationalUncertainty.HIGH
						: OperationalUncertainty.LOW
		);
	}

	private EvidenceGovernancePolicy governancePolicy(
			EvidenceTrustLevel trustLevel,
			EvidenceIntegrityStatus integrityStatus,
			EvidenceClassification classification,
			EvidenceProvenance provenance
	) {
		return new EvidenceGovernancePolicy(
				trustLevel,
				integrityStatus,
				classification,
				provenance
		);
	}

	private EvidenceProvenance provenance(
			boolean sanitized,
			boolean rawPayloadPresent,
			boolean sensitiveDataPresent
	) {
		return new EvidenceProvenance(
				EvidenceSourceType.METRICS,
				"adapter-1",
				Instant.parse("2026-06-03T00:00:00Z"),
				sanitized,
				rawPayloadPresent,
				sensitiveDataPresent
		);
	}

	private EvidenceRuntimeApiResponse apiResponse() {
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
}
