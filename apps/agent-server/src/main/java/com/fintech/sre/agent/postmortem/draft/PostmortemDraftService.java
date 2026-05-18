package com.fintech.sre.agent.postmortem.draft;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.observability.metrics.LearningMetricsRecorder;
import com.fintech.sre.agent.reanalysis.ReanalysisCandidateStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Mono;

@Service("internalPostmortemDraftService")
public class PostmortemDraftService {

	private final PostmortemDraftStore draftStore;
	private final PostmortemDraftIdGenerator idGenerator;
	private final IncidentLifecycleStore lifecycleStore;
	private final RecommendationRecordStore recommendationStore;
	private final RecommendationApprovalStore approvalStore;
	private final HumanExecutionResultStore executionResultStore;
	private final VerificationResultStore verificationResultStore;
	private final ReanalysisCandidateStore reanalysisCandidateStore;
	private final LearningMetricsRecorder metricsRecorder;

	public PostmortemDraftService(
			PostmortemDraftStore draftStore,
			PostmortemDraftIdGenerator idGenerator,
			IncidentLifecycleStore lifecycleStore,
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationResultStore,
			ReanalysisCandidateStore reanalysisCandidateStore,
			LearningMetricsRecorder metricsRecorder
	) {
		this.draftStore = draftStore;
		this.idGenerator = idGenerator;
		this.lifecycleStore = lifecycleStore;
		this.recommendationStore = recommendationStore;
		this.approvalStore = approvalStore;
		this.executionResultStore = executionResultStore;
		this.verificationResultStore = verificationResultStore;
		this.reanalysisCandidateStore = reanalysisCandidateStore;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<PostmortemDraftResponse> create(
			String incidentId,
			PostmortemDraftRequest request
	) {
		return validate(request)
				.then(lifecycleStore.findLatestByIncidentId(incidentId))
				.switchIfEmpty(Mono.error(new PostmortemDraftRejectedException(
						"INCIDENT_LIFECYCLE_NOT_FOUND",
						"Incident lifecycle record not found."
				)))
				.flatMap(latest -> {
					if (latest.currentStatus() != IncidentStatus.RESOLVED) {
						return Mono.error(new PostmortemDraftRejectedException(
								"INCIDENT_NOT_RESOLVED",
								"Postmortem draft can be created only for RESOLVED incidents."
						));
					}

					return buildDraft(incidentId, request);
				});
	}

	public Mono<PostmortemDraftRecord> findById(String postmortemDraftId) {
		return draftStore.findById(postmortemDraftId);
	}

	private Mono<Void> validate(PostmortemDraftRequest request) {
		if (request == null) {
			return Mono.error(new PostmortemDraftRejectedException(
					"POSTMORTEM_DRAFT_REQUEST_REQUIRED",
					"Postmortem draft request is required."
			));
		}

		if (request.requestedBy() == null || request.requestedBy().isBlank()) {
			return Mono.error(new PostmortemDraftRejectedException(
					"REQUESTED_BY_REQUIRED",
					"requestedBy is required."
			));
		}

		if (request.reason() == null || request.reason().isBlank()) {
			return Mono.error(new PostmortemDraftRejectedException(
					"POSTMORTEM_DRAFT_REASON_REQUIRED",
					"reason is required."
			));
		}

		return Mono.empty();
	}

	private Mono<PostmortemDraftResponse> buildDraft(
			String incidentId,
			PostmortemDraftRequest request
	) {
		return Mono.zip(
				lifecycleStore.findByIncidentId(incidentId).collectList(),
				recommendationStore.findByIncidentId(incidentId).collectList(),
				approvalStore.findByIncidentId(incidentId).collectList(),
				executionResultStore.findByIncidentId(incidentId).collectList(),
				verificationResultStore.findByIncidentId(incidentId).collectList(),
				reanalysisCandidateStore.findByIncidentId(incidentId).collectList()
		).flatMap(tuple -> {
			List<String> timeline = new ArrayList<>();
			tuple.getT1().forEach(record -> timeline.add(
					record.transitionedAt() + " incident transitioned to "
							+ record.currentStatus() + " because " + record.transitionReason()
			));

			List<String> recommendations = tuple.getT2().stream()
					.map(record -> "Recommendation " + record.recommendationRecordId()
							+ " policy=" + record.policyDecision()
							+ " guardrail=" + record.guardrailDecision()
							+ " actions=" + record.actionTypes())
					.toList();

			List<String> approvals = tuple.getT3().stream()
					.map(record -> "Approval " + record.status()
							+ " by " + record.operatorId()
							+ ": " + record.reason())
					.toList();

			List<String> executionResults = tuple.getT4().stream()
					.map(record -> "Human execution " + record.status()
							+ " by " + record.operatorId()
							+ ": " + record.summary())
					.toList();

			List<String> verificationResults = tuple.getT5().stream()
					.map(record -> "Verification " + record.status()
							+ " by " + record.operatorId()
							+ ": " + record.summary())
					.toList();

			List<String> reanalysisCandidates = tuple.getT6().stream()
					.map(record -> "Reanalysis candidate " + record.reason()
							+ ": " + record.summary())
					.toList();

			List<String> learningCandidates = List.of(
					"Review whether scenario/runbook coverage was sufficient.",
					"Review whether rollback and verification steps were actionable.",
					"Review whether SLO thresholds and alert routing were appropriate."
			);

			List<String> openQuestions = List.of(
					"Root cause must be verified by human reviewer.",
					"Confirm whether payment integrity or duplicate payment risk existed.",
					"Confirm whether preventive design should be updated."
			);

			String summary = "Postmortem draft for incident " + incidentId
					+ ". This is a human-review draft and does not assert root cause certainty.";

			PostmortemDraftRecord draft = new PostmortemDraftRecord(
					idGenerator.generate(),
					incidentId,
					PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
					request.requestedBy(),
					summary,
					timeline,
					recommendations,
					merge(approvals, executionResults),
					verificationResults,
					reanalysisCandidates,
					learningCandidates,
					openQuestions,
					Instant.now(),
					sanitizeMetadata(request.metadata())
			);

			return draftStore.save(draft)
					.doOnNext(metricsRecorder::recordPostmortemDraft)
					.map(this::toResponse);
		});
	}

	private List<String> merge(List<String> left, List<String> right) {
		List<String> merged = new ArrayList<>();
		merged.addAll(left);
		merged.addAll(right);
		return List.copyOf(merged);
	}

	private PostmortemDraftResponse toResponse(PostmortemDraftRecord record) {
		return new PostmortemDraftResponse(
				record.postmortemDraftId(),
				record.incidentId(),
				record.status(),
				record.summary(),
				record.openQuestions()
		);
	}

	private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return Map.of();
		}

		return metadata.entrySet().stream()
				.filter(entry -> allowed(entry.getKey()))
				.collect(Collectors.toUnmodifiableMap(
						Map.Entry::getKey,
						Map.Entry::getValue
				));
	}

	private boolean allowed(String key) {
		if (key == null) {
			return false;
		}

		String lower = key.toLowerCase();

		return !lower.contains("payload")
				&& !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("payment")
				&& !lower.contains("prompt")
				&& !lower.contains("rawlog");
	}
}
