package com.fintech.sre.agent.learning.promotion;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.observability.metrics.LearningMetricsRecorder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class KnowledgePromotionReviewService {

	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionReviewStore reviewStore;
	private final KnowledgePromotionReviewIdGenerator idGenerator;
	private final LearningMetricsRecorder metricsRecorder;

	public KnowledgePromotionReviewService(
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionReviewStore reviewStore,
			KnowledgePromotionReviewIdGenerator idGenerator,
			LearningMetricsRecorder metricsRecorder
	) {
		this.learningCandidateStore = learningCandidateStore;
		this.reviewStore = reviewStore;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<KnowledgePromotionReviewResponse> review(
			String learningCandidateId,
			KnowledgePromotionReviewRequest request
	) {
		return validate(request)
				.then(learningCandidateStore.findById(learningCandidateId))
				.switchIfEmpty(Mono.error(new KnowledgePromotionReviewRejectedException(
						"LEARNING_CANDIDATE_NOT_FOUND",
						"Learning candidate not found."
				)))
				.flatMap(candidate -> validateCandidate(candidate)
						.then(save(candidate, request)));
	}

	public Mono<KnowledgePromotionReviewRecord> latest(String learningCandidateId) {
		return reviewStore.findLatestByLearningCandidateId(learningCandidateId);
	}

	public Flux<KnowledgePromotionReviewRecord> historyByIncident(String incidentId) {
		return reviewStore.findByIncidentId(incidentId);
	}

	private Mono<Void> validate(KnowledgePromotionReviewRequest request) {
		if (request == null) {
			return Mono.error(new KnowledgePromotionReviewRejectedException(
					"KNOWLEDGE_PROMOTION_REVIEW_REQUEST_REQUIRED",
					"Review request is required."
			));
		}

		if (request.status() == null) {
			return Mono.error(new KnowledgePromotionReviewRejectedException(
					"KNOWLEDGE_PROMOTION_REVIEW_STATUS_REQUIRED",
					"status is required."
			));
		}

		if (request.reviewedBy() == null || request.reviewedBy().isBlank()) {
			return Mono.error(new KnowledgePromotionReviewRejectedException(
					"KNOWLEDGE_PROMOTION_REVIEWER_REQUIRED",
					"reviewedBy is required."
			));
		}

		if (request.reviewSummary() == null || request.reviewSummary().isBlank()) {
			return Mono.error(new KnowledgePromotionReviewRejectedException(
					"KNOWLEDGE_PROMOTION_REVIEW_SUMMARY_REQUIRED",
					"reviewSummary is required."
			));
		}

		return Mono.empty();
	}

	private Mono<Void> validateCandidate(LearningCandidateRecord candidate) {
		if (candidate.status() != LearningCandidateStatus.REVIEW_REQUIRED) {
			return Mono.error(new KnowledgePromotionReviewRejectedException(
					"LEARNING_CANDIDATE_NOT_REVIEW_REQUIRED",
					"Only REVIEW_REQUIRED learning candidates can be reviewed for promotion."
			));
		}

		return Mono.empty();
	}

	private Mono<KnowledgePromotionReviewResponse> save(
			LearningCandidateRecord candidate,
			KnowledgePromotionReviewRequest request
	) {
		KnowledgePromotionReviewRecord record = new KnowledgePromotionReviewRecord(
				idGenerator.generate(),
				candidate.learningCandidateId(),
				candidate.incidentId(),
				request.status(),
				request.reviewedBy(),
				request.reviewReason(),
				request.reviewSummary(),
				Instant.now(),
				sanitizeMetadata(request.metadata())
		);

		return reviewStore.save(record)
				.doOnNext(metricsRecorder::recordPromotionReview)
				.map(this::toResponse);
	}

	private KnowledgePromotionReviewResponse toResponse(KnowledgePromotionReviewRecord record) {
		return new KnowledgePromotionReviewResponse(
				record.promotionReviewId(),
				record.learningCandidateId(),
				record.incidentId(),
				record.status(),
				record.reviewedBy(),
				record.reviewSummary()
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
