package com.fintech.sre.agent.postmortem.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.incident.lifecycle.InMemoryIncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleIdGenerator;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.reanalysis.InMemoryReanalysisCandidateStore;
import com.fintech.sre.agent.reanalysis.ReanalysisCandidateStatus;
import com.fintech.sre.agent.reanalysis.ReanalysisTriggerCandidate;
import com.fintech.sre.agent.reanalysis.ReanalysisTriggerReason;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

class PostmortemDraftServiceTest {

	@Test
	void shouldCreateDraftForResolvedIncident() {
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		InMemoryRecommendationApprovalStore approvalStore = new InMemoryRecommendationApprovalStore();
		InMemoryHumanExecutionResultStore executionResultStore = new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationResultStore = new InMemoryVerificationResultStore();
		InMemoryReanalysisCandidateStore reanalysisCandidateStore = new InMemoryReanalysisCandidateStore();

		lifecycleStore.save(lifecycleRecord("incident-1", IncidentStatus.RESOLVED)).block();
		recommendationStore.save(recommendationRecord()).block();
		approvalStore.save(approvalRecord()).block();
		executionResultStore.save(executionRecord()).block();
		verificationResultStore.save(verificationRecord()).block();
		reanalysisCandidateStore.save(reanalysisRecord()).block();

		PostmortemDraftService service = new PostmortemDraftService(
				new InMemoryPostmortemDraftStore(),
				new PostmortemDraftIdGenerator(),
				lifecycleStore,
				recommendationStore,
				approvalStore,
				executionResultStore,
				verificationResultStore,
				reanalysisCandidateStore,
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		PostmortemDraftResponse response = service.create(
				"incident-1",
				new PostmortemDraftRequest(
						"operator-a",
						"prepare draft",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.status()).isEqualTo(PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED);
		assertThat(response.summary()).contains("does not assert root cause certainty");
		assertThat(response.openQuestions()).isNotEmpty();
	}

	@Test
	void shouldRejectWhenIncidentIsNotResolved() {
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		lifecycleStore.save(lifecycleRecord("incident-2", IncidentStatus.OPEN)).block();

		PostmortemDraftService service = new PostmortemDraftService(
				new InMemoryPostmortemDraftStore(),
				new PostmortemDraftIdGenerator(),
				lifecycleStore,
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryReanalysisCandidateStore(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		assertThatThrownBy(() -> service.create(
				"incident-2",
				new PostmortemDraftRequest("operator-a", "prepare draft", Map.of())
		).block())
				.isInstanceOf(PostmortemDraftRejectedException.class)
				.hasMessage("Postmortem draft can be created only for RESOLVED incidents.");
	}

	@Test
	void shouldSanitizeMetadata() {
		InMemoryIncidentLifecycleStore lifecycleStore = new InMemoryIncidentLifecycleStore();
		InMemoryPostmortemDraftStore draftStore = new InMemoryPostmortemDraftStore();
		lifecycleStore.save(lifecycleRecord("incident-3", IncidentStatus.RESOLVED)).block();

		PostmortemDraftService service = new PostmortemDraftService(
				draftStore,
				new PostmortemDraftIdGenerator(),
				lifecycleStore,
				new InMemoryRecommendationRecordStore(),
				new InMemoryRecommendationApprovalStore(),
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new InMemoryReanalysisCandidateStore(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		PostmortemDraftResponse response = service.create(
				"incident-3",
				new PostmortemDraftRequest(
						"operator-a",
						"prepare draft",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store",
								"rawLog", "must-not-store"
						)
				)
		).block();

		PostmortemDraftRecord stored = draftStore.findById(response.postmortemDraftId()).block();
		assertThat(stored.metadata()).containsKey("team");
		assertThat(stored.metadata()).doesNotContainKey("paymentPayload");
		assertThat(stored.metadata()).doesNotContainKey("rawLog");
	}

	private IncidentLifecycleRecord lifecycleRecord(String incidentId, IncidentStatus status) {
		return new IncidentLifecycleRecord(
				new IncidentLifecycleIdGenerator().generate(),
				incidentId,
				status == IncidentStatus.RESOLVED ? IncidentStatus.STABILIZING : null,
				status,
				IncidentTransitionReason.INCIDENT_RESOLVED,
				"operator-a",
				"lifecycle update",
				Instant.now(),
				Map.of()
		);
	}

	private RecommendationRecord recommendationRecord() {
		return new RecommendationRecord(
				"recommendation-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				Instant.now(),
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of("alertName", "HighP99Latency")
		);
	}

	private RecommendationApprovalRecord approvalRecord() {
		return new RecommendationApprovalRecord(
				"approval-1",
				"recommendation-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-a",
				"looks safe",
				Instant.now(),
				Map.of()
		);
	}

	private HumanExecutionResultRecord executionRecord() {
		return new HumanExecutionResultRecord(
				"execution-result-1",
				"execution-plan-1",
				"recommendation-1",
				"incident-1",
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"manual action applied",
				Instant.now().minusSeconds(60),
				Instant.now().minusSeconds(30),
				Instant.now(),
				Map.of()
		);
	}

	private VerificationResultRecord verificationRecord() {
		return new VerificationResultRecord(
				"verification-1",
				"execution-result-1",
				"execution-plan-1",
				"recommendation-1",
				"incident-1",
				VerificationStatus.VERIFIED,
				"operator-a",
				"Latency normalized",
				Instant.now(),
				Map.of()
		);
	}

	private ReanalysisTriggerCandidate reanalysisRecord() {
		return new ReanalysisTriggerCandidate(
				"candidate-1",
				"incident-1",
				"verification-1",
				"execution-result-1",
				ReanalysisTriggerReason.STABILIZATION_FAILED,
				ReanalysisCandidateStatus.PENDING_REANALYSIS,
				"operator-a",
				"Needs follow-up review",
				Instant.now(),
				Map.of()
		);
	}
}
