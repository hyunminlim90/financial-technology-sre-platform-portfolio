package com.fintech.sre.agent.learning.candidate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.LearningMetricsRecorder;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LearningCandidatePromotionService {

	private final LearningCandidateStore candidateStore;
	private final LearningCandidateIdGenerator idGenerator;
	private final PostmortemDraftStore draftStore;
	private final PostmortemReviewStore reviewStore;
	private final LearningMetricsRecorder metricsRecorder;

	public LearningCandidatePromotionService(
			LearningCandidateStore candidateStore,
			LearningCandidateIdGenerator idGenerator,
			PostmortemDraftStore draftStore,
			PostmortemReviewStore reviewStore,
			LearningMetricsRecorder metricsRecorder
	) {
		this.candidateStore = candidateStore;
		this.idGenerator = idGenerator;
		this.draftStore = draftStore;
		this.reviewStore = reviewStore;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<LearningCandidatePromotionResponse> promote(
			String postmortemDraftId,
			LearningCandidatePromotionRequest request
	) {
		return validate(request)
				.then(draftStore.findById(postmortemDraftId))
				.switchIfEmpty(Mono.error(
						new LearningCandidateRejectedException(
								"POSTMORTEM_DRAFT_NOT_FOUND",
								"Postmortem draft not found."
						)
				))
				.flatMap(draft ->
						validateDraftApproved(draft)
								.then(findLatestApprovedReview(draft))
								.flatMap(review ->
										saveCandidate(
												draft,
												review,
												request
										)
								));
	}

	public Mono<LearningCandidateRecord> findById(
			String learningCandidateId
	) {
		return candidateStore.findById(learningCandidateId);
	}

	public Flux<LearningCandidateRecord> findByIncidentId(
			String incidentId
	) {
		return candidateStore.findByIncidentId(incidentId);
	}

	private Mono<Void> validate(
			LearningCandidatePromotionRequest request
	) {
		if (request == null) {
			return Mono.error(
					new LearningCandidateRejectedException(
							"LEARNING_CANDIDATE_REQUEST_REQUIRED",
							"Learning candidate request is required."
					)
			);
		}

		if (request.type() == null) {
			return Mono.error(
					new LearningCandidateRejectedException(
							"LEARNING_CANDIDATE_TYPE_REQUIRED",
							"type is required."
					)
			);
		}

		if (request.promotedBy() == null
				|| request.promotedBy().isBlank()) {
			return Mono.error(
					new LearningCandidateRejectedException(
							"LEARNING_CANDIDATE_PROMOTER_REQUIRED",
							"promotedBy is required."
					)
			);
		}

		if (request.summary() == null
				|| request.summary().isBlank()) {
			return Mono.error(
					new LearningCandidateRejectedException(
							"LEARNING_CANDIDATE_SUMMARY_REQUIRED",
							"summary is required."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<Void> validateDraftApproved(
			PostmortemDraftRecord draft
	) {
		if (draft.status() != PostmortemDraftStatus.APPROVED) {
			return Mono.error(
					new LearningCandidateRejectedException(
							"POSTMORTEM_DRAFT_NOT_APPROVED",
							"Postmortem draft must be APPROVED."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<PostmortemReviewRecord> findLatestApprovedReview(
			PostmortemDraftRecord draft
	) {
		return reviewStore.findLatestByDraftId(
						draft.postmortemDraftId()
				)
				.switchIfEmpty(Mono.error(
						new LearningCandidateRejectedException(
								"POSTMORTEM_REVIEW_NOT_FOUND",
								"Postmortem review not found."
						)
				))
				.flatMap(review -> {
					if (review.status()
							!= PostmortemReviewStatus.APPROVED) {
						return Mono.error(
								new LearningCandidateRejectedException(
										"POSTMORTEM_REVIEW_NOT_APPROVED",
										"Latest postmortem review must be APPROVED."
								)
						);
					}

					return Mono.just(review);
				});
	}

	private Mono<LearningCandidatePromotionResponse> saveCandidate(
			PostmortemDraftRecord draft,
			PostmortemReviewRecord review,
			LearningCandidatePromotionRequest request
	) {
		LearningCandidateRecord record =
				new LearningCandidateRecord(
						idGenerator.generate(),
						draft.incidentId(),
						draft.postmortemDraftId(),
						review.postmortemReviewId(),
						request.type(),
						LearningCandidateStatus.REVIEW_REQUIRED,
						request.promotedBy(),
						request.summary(),
						sanitizeProposedChanges(
								request.proposedChanges()
						),
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return candidateStore.save(record)
				.doOnNext(metricsRecorder::recordLearningCandidate)
				.map(this::toResponse);
	}

	private LearningCandidatePromotionResponse toResponse(
			LearningCandidateRecord record
	) {
		return new LearningCandidatePromotionResponse(
				record.learningCandidateId(),
				record.incidentId(),
				record.postmortemDraftId(),
				record.type(),
				record.status(),
				record.summary()
		);
	}

	private List<String> sanitizeProposedChanges(
			List<String> changes
	) {
		if (changes == null || changes.isEmpty()) {
			return List.of();
		}

		return changes.stream()
				.filter(change -> {
					String lower = change.toLowerCase();

					return !lower.contains("payment payload")
							&& !lower.contains("customer")
							&& !lower.contains("secret")
							&& !lower.contains("token")
							&& !lower.contains("raw log")
							&& !lower.contains("full prompt");
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
