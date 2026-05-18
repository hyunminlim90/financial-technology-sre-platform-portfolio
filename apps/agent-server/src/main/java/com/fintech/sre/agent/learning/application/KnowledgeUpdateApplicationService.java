package com.fintech.sre.agent.learning.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.observability.metrics.LearningMetricsRecorder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class KnowledgeUpdateApplicationService {

	private final KnowledgePromotionPlanStore planStore;
	private final KnowledgeUpdateApplicationStore applicationStore;
	private final KnowledgeUpdateApplicationIdGenerator idGenerator;
	private final LearningMetricsRecorder metricsRecorder;

	public KnowledgeUpdateApplicationService(
			KnowledgePromotionPlanStore planStore,
			KnowledgeUpdateApplicationStore applicationStore,
			KnowledgeUpdateApplicationIdGenerator idGenerator,
			LearningMetricsRecorder metricsRecorder
	) {
		this.planStore = planStore;
		this.applicationStore = applicationStore;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<KnowledgeUpdateApplicationResponse> apply(
			String learningCandidateId,
			KnowledgeUpdateApplicationRequest request
	) {
		return validate(request)
				.then(planStore.findById(request.promotionPlanId()))
				.switchIfEmpty(Mono.error(
						new KnowledgeUpdateApplicationRejectedException(
								"PROMOTION_PLAN_NOT_FOUND",
								"Promotion plan not found."
						)
				))
				.flatMap(plan ->
						save(
								learningCandidateId,
								plan,
								request
						));
	}

	public Mono<KnowledgeUpdateApplicationRecord> findById(
			String knowledgeUpdateApplicationId
	) {
		return applicationStore.findById(
				knowledgeUpdateApplicationId
		);
	}

	public Flux<KnowledgeUpdateApplicationRecord> findByIncidentId(
			String incidentId
	) {
		return applicationStore.findByIncidentId(incidentId);
	}

	private Mono<Void> validate(
			KnowledgeUpdateApplicationRequest request
	) {
		if (request == null) {
			return Mono.error(
					new KnowledgeUpdateApplicationRejectedException(
							"KNOWLEDGE_UPDATE_REQUEST_REQUIRED",
							"Knowledge update request is required."
					)
			);
		}

		if (blank(request.filePath())) {
			return Mono.error(
					new KnowledgeUpdateApplicationRejectedException(
							"KNOWLEDGE_UPDATE_FILE_PATH_REQUIRED",
							"filePath is required."
					)
			);
		}

		if (blank(request.gitCommitSha())) {
			return Mono.error(
					new KnowledgeUpdateApplicationRejectedException(
							"KNOWLEDGE_UPDATE_GIT_COMMIT_REQUIRED",
							"gitCommitSha is required."
					)
			);
		}

		if (blank(request.appliedBy())) {
			return Mono.error(
					new KnowledgeUpdateApplicationRejectedException(
							"KNOWLEDGE_UPDATE_APPLIED_BY_REQUIRED",
							"appliedBy is required."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<KnowledgeUpdateApplicationResponse> save(
			String learningCandidateId,
			KnowledgePromotionPlanRecord plan,
			KnowledgeUpdateApplicationRequest request
	) {
		KnowledgeUpdateApplicationRecord record =
				new KnowledgeUpdateApplicationRecord(
						idGenerator.generate(),
						plan.incidentId(),
						learningCandidateId,
						request.promotionPlanId(),
						request.knowledgeType(),
						request.knowledgeLayer(),
						request.filePath(),
						request.changeType(),
						request.gitRepository(),
						request.gitBranch(),
						request.gitCommitSha(),
						request.pullRequestReference(),
						request.appliedBy(),
						request.reviewedBy(),
						request.approvedBy(),
						sanitizeValidationChecks(
								request.validationChecks()
						),
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return applicationStore.save(record)
				.doOnNext(metricsRecorder::recordKnowledgeUpdate)
				.map(this::toResponse);
	}

	private KnowledgeUpdateApplicationResponse toResponse(
			KnowledgeUpdateApplicationRecord record
	) {
		return new KnowledgeUpdateApplicationResponse(
				record.knowledgeUpdateApplicationId(),
				record.incidentId(),
				record.learningCandidateId(),
				record.promotionPlanId(),
				record.filePath(),
				record.gitCommitSha(),
				record.appliedBy()
		);
	}

	private List<String> sanitizeValidationChecks(
			List<String> checks
	) {
		if (checks == null || checks.isEmpty()) {
			return List.of();
		}

		return checks.stream()
				.filter(check -> {
					String lower = check.toLowerCase();

					return !lower.contains("customer")
							&& !lower.contains("secret")
							&& !lower.contains("token")
							&& !lower.contains("payment payload")
							&& !lower.contains("raw log");
				})
				.toList();
	}

	private Map<String, String> sanitizeMetadata(
			Map<String, String> metadata
	) {
		if (metadata == null || metadata.isEmpty()) {
			return Map.of();
		}

		return metadata.entrySet().stream()
				.filter(entry -> allowed(entry.getKey()))
				.collect(java.util.stream.Collectors.toUnmodifiableMap(
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

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
