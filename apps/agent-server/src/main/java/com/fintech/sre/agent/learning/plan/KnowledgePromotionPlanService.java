package com.fintech.sre.agent.learning.plan;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.observability.metrics.LearningMetricsRecorder;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class KnowledgePromotionPlanService {

	private final LearningCandidateStore candidateStore;
	private final KnowledgePromotionReviewStore reviewStore;
	private final KnowledgePromotionPlanStore planStore;
	private final KnowledgePromotionTargetPlanner targetPlanner;
	private final KnowledgePromotionPlanIdGenerator idGenerator;
	private final LearningMetricsRecorder metricsRecorder;

	public KnowledgePromotionPlanService(
			LearningCandidateStore candidateStore,
			KnowledgePromotionReviewStore reviewStore,
			KnowledgePromotionPlanStore planStore,
			KnowledgePromotionTargetPlanner targetPlanner,
			KnowledgePromotionPlanIdGenerator idGenerator,
			LearningMetricsRecorder metricsRecorder
	) {
		this.candidateStore = candidateStore;
		this.reviewStore = reviewStore;
		this.planStore = planStore;
		this.targetPlanner = targetPlanner;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<KnowledgePromotionPlanResponse> createPlan(
			String learningCandidateId,
			KnowledgePromotionPlanRequest request
	) {
		return validate(request)
				.then(candidateStore.findById(learningCandidateId))
				.switchIfEmpty(Mono.error(new KnowledgePromotionPlanRejectedException(
						"LEARNING_CANDIDATE_NOT_FOUND",
						"Learning candidate not found."
				)))
				.flatMap(candidate ->
						reviewStore.findLatestByLearningCandidateId(learningCandidateId)
								.switchIfEmpty(Mono.error(new KnowledgePromotionPlanRejectedException(
										"PROMOTION_REVIEW_NOT_FOUND",
										"Knowledge promotion review not found."
								)))
								.flatMap(review -> {
									if (review.status() != KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION) {
										return Mono.error(new KnowledgePromotionPlanRejectedException(
												"PROMOTION_REVIEW_NOT_APPROVED",
												"Latest promotion review must be APPROVED_FOR_PROMOTION."
										));
									}

									return savePlan(candidate, request);
								})
				);
	}

	public Mono<KnowledgePromotionPlanRecord> findById(String id) {
		return planStore.findById(id);
	}

	public Flux<KnowledgePromotionPlanRecord> findByIncidentId(String incidentId) {
		return planStore.findByIncidentId(incidentId);
	}

	private Mono<Void> validate(KnowledgePromotionPlanRequest request) {
		if (request == null) {
			return Mono.error(new KnowledgePromotionPlanRejectedException(
					"KNOWLEDGE_PROMOTION_PLAN_REQUEST_REQUIRED",
					"Knowledge promotion plan request is required."
			));
		}

		if (request.plannedBy() == null || request.plannedBy().isBlank()) {
			return Mono.error(new KnowledgePromotionPlanRejectedException(
					"KNOWLEDGE_PROMOTION_PLANNER_REQUIRED",
					"plannedBy is required."
			));
		}

		if (request.summary() == null || request.summary().isBlank()) {
			return Mono.error(new KnowledgePromotionPlanRejectedException(
					"KNOWLEDGE_PROMOTION_PLAN_SUMMARY_REQUIRED",
					"summary is required."
			));
		}

		return Mono.empty();
	}

	private Mono<KnowledgePromotionPlanResponse> savePlan(
			LearningCandidateRecord candidate,
			KnowledgePromotionPlanRequest request
	) {
		List<KnowledgePromotionPlanTarget> targets =
				targetPlanner.planTargets(candidate);

		List<String> blockedReasons = blockedReasons(targets);

		KnowledgePromotionPlanRecord record =
				new KnowledgePromotionPlanRecord(
						idGenerator.generate(),
						candidate.learningCandidateId(),
						candidate.incidentId(),
						blockedReasons.isEmpty()
								? KnowledgePromotionPlanStatus.PLAN_CREATED
								: KnowledgePromotionPlanStatus.BLOCKED,
						request.plannedBy(),
						request.summary(),
						targets,
						requiredHumanChecks(),
						blockedReasons,
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return planStore.save(record)
				.doOnNext(metricsRecorder::recordPromotionPlan)
				.map(this::toResponse);
	}

	private List<String> blockedReasons(List<KnowledgePromotionPlanTarget> targets) {
		if (targets == null || targets.isEmpty()) {
			return List.of("NO_PROMOTION_TARGET_AVAILABLE");
		}

		boolean missingPath = targets.stream()
				.anyMatch(target -> target.recommendedPath() == null
						|| target.recommendedPath().isBlank());

		if (missingPath) {
			return List.of("PROMOTION_TARGET_PATH_REQUIRED");
		}

		return List.of();
	}

	private List<String> requiredHumanChecks() {
		return List.of(
				"Human must edit portfolio knowledge files manually.",
				"Human must verify scenario/runbook/policy consistency.",
				"Human must ensure no payment payload, customer data, secrets, raw logs, or prompts are included.",
				"Human must run knowledge validation before ingestion.",
				"Human must create Git commit/PR outside agent-server."
		);
	}

	private KnowledgePromotionPlanResponse toResponse(KnowledgePromotionPlanRecord record) {
		return new KnowledgePromotionPlanResponse(
				record.promotionPlanId(),
				record.learningCandidateId(),
				record.incidentId(),
				record.status(),
				record.summary(),
				record.targets(),
				record.requiredHumanChecks(),
				record.blockedReasons()
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
				&& !lower.contains("rawlog")
				&& !lower.contains("password");
	}
}
