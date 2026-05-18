package com.fintech.sre.agent.postmortem.review;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.LearningMetricsRecorder;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service("internalPostmortemReviewService")
public class PostmortemReviewService {

	private final PostmortemReviewStore reviewStore;
	private final PostmortemDraftStore draftStore;
	private final PostmortemReviewIdGenerator idGenerator;
	private final LearningMetricsRecorder metricsRecorder;

	public PostmortemReviewService(
			PostmortemReviewStore reviewStore,
			PostmortemDraftStore draftStore,
			PostmortemReviewIdGenerator idGenerator,
			LearningMetricsRecorder metricsRecorder
	) {
		this.reviewStore = reviewStore;
		this.draftStore = draftStore;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<PostmortemReviewResponse> review(
			String draftId,
			PostmortemReviewRequest request
	) {
		return validate(request)
				.then(draftStore.findById(draftId))
				.switchIfEmpty(Mono.error(
						new PostmortemReviewRejectedException(
								"POSTMORTEM_DRAFT_NOT_FOUND",
								"Postmortem draft not found."
						)
				))
				.flatMap(draft -> createReview(draft, request));
	}

	public Mono<PostmortemReviewRecord> latest(
			String draftId
	) {
		return reviewStore.findLatestByDraftId(draftId);
	}

	public Flux<PostmortemReviewRecord> history(
			String incidentId
	) {
		return reviewStore.findByIncidentId(incidentId);
	}

	private Mono<Void> validate(
			PostmortemReviewRequest request
	) {
		if (request == null) {
			return Mono.error(
					new PostmortemReviewRejectedException(
							"POSTMORTEM_REVIEW_REQUEST_REQUIRED",
							"Review request is required."
					)
			);
		}

		if (request.status() == null) {
			return Mono.error(
					new PostmortemReviewRejectedException(
							"POSTMORTEM_REVIEW_STATUS_REQUIRED",
							"status is required."
					)
			);
		}

		if (request.reviewedBy() == null
				|| request.reviewedBy().isBlank()) {
			return Mono.error(
					new PostmortemReviewRejectedException(
							"POSTMORTEM_REVIEWER_REQUIRED",
							"reviewedBy is required."
					)
			);
		}

		if (request.reviewSummary() == null
				|| request.reviewSummary().isBlank()) {
			return Mono.error(
					new PostmortemReviewRejectedException(
							"POSTMORTEM_REVIEW_SUMMARY_REQUIRED",
							"reviewSummary is required."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<PostmortemReviewResponse> createReview(
			PostmortemDraftRecord draft,
			PostmortemReviewRequest request
	) {
		PostmortemReviewRecord review =
				new PostmortemReviewRecord(
						idGenerator.generate(),
						draft.postmortemDraftId(),
						draft.incidentId(),
						request.status(),
						request.reviewedBy(),
						request.reviewReason(),
						request.reviewSummary(),
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return reviewStore.save(review)
				.doOnNext(metricsRecorder::recordPostmortemReview)
				.flatMap(saved -> updateDraftStatus(draft, request.status())
						.thenReturn(saved))
				.map(this::toResponse);
	}

	private Mono<Void> updateDraftStatus(
			PostmortemDraftRecord draft,
			PostmortemReviewStatus reviewStatus
	) {
		PostmortemDraftStatus next =
				switch (reviewStatus) {
					case APPROVED -> PostmortemDraftStatus.APPROVED;
					case REJECTED -> PostmortemDraftStatus.REJECTED;
					case NEEDS_REVISION ->
							PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED;
					case PENDING_REVIEW ->
							PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED;
				};

		PostmortemDraftRecord updated =
				new PostmortemDraftRecord(
						draft.postmortemDraftId(),
						draft.incidentId(),
						next,
						draft.requestedBy(),
						draft.summary(),
						draft.timeline(),
						draft.recommendations(),
						draft.executionResults(),
						draft.verificationResults(),
						draft.reanalysisCandidates(),
						draft.learningCandidates(),
						draft.openQuestions(),
						draft.createdAt(),
						draft.metadata()
				);

		return draftStore.save(updated).then();
	}

	private PostmortemReviewResponse toResponse(
			PostmortemReviewRecord review
	) {
		return new PostmortemReviewResponse(
				review.postmortemReviewId(),
				review.postmortemDraftId(),
				review.incidentId(),
				review.status(),
				review.reviewedBy(),
				review.reviewSummary()
		);
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
