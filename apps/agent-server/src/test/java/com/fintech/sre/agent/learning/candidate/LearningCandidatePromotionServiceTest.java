package com.fintech.sre.agent.learning.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.postmortem.draft.InMemoryPostmortemDraftStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;

class LearningCandidatePromotionServiceTest {

	@Test
	void shouldPromoteApprovedDraftAndReview() {
		InMemoryLearningCandidateStore candidateStore = new InMemoryLearningCandidateStore();
		InMemoryPostmortemDraftStore draftStore = new InMemoryPostmortemDraftStore();
		InMemoryPostmortemReviewStore reviewStore = new InMemoryPostmortemReviewStore();

		draftStore.save(draft(PostmortemDraftStatus.APPROVED)).block();
		reviewStore.save(review(PostmortemReviewStatus.APPROVED)).block();

		LearningCandidatePromotionService service = new LearningCandidatePromotionService(
				candidateStore,
				new LearningCandidateIdGenerator(),
				draftStore,
				reviewStore,
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		LearningCandidatePromotionResponse response = service.promote(
				"draft-1",
				new LearningCandidatePromotionRequest(
						LearningCandidateType.RUNBOOK_UPDATE,
						"operator-a",
						"Promote reviewed runbook changes.",
						List.of(
								"Add verification checklist.",
								"payment payload dump"
						),
						Map.of(
								"team", "sre",
								"secret", "must-not-store"
						)
				)
		).block();

		assertThat(response.status())
				.isEqualTo(
						LearningCandidateStatus.REVIEW_REQUIRED
				);

		LearningCandidateRecord record = candidateStore.findById(response.learningCandidateId()).block();
		assertThat(record.proposedChanges())
				.doesNotContain(
						"payment payload dump"
				);
		assertThat(record.metadata())
				.containsKey("team")
				.doesNotContainKey("secret");
	}

	@Test
	void shouldRejectWhenLatestReviewNotApproved() {
		InMemoryPostmortemDraftStore draftStore = new InMemoryPostmortemDraftStore();
		InMemoryPostmortemReviewStore reviewStore = new InMemoryPostmortemReviewStore();

		draftStore.save(draft(PostmortemDraftStatus.APPROVED)).block();
		reviewStore.save(review(PostmortemReviewStatus.NEEDS_REVISION)).block();

		LearningCandidatePromotionService service = new LearningCandidatePromotionService(
				new InMemoryLearningCandidateStore(),
				new LearningCandidateIdGenerator(),
				draftStore,
				reviewStore,
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		assertThatThrownBy(() -> service.promote(
				"draft-1",
				new LearningCandidatePromotionRequest(
						LearningCandidateType.SCENARIO_UPDATE,
						"operator-a",
						"summary",
						List.of("Change scenario threshold."),
						Map.of()
				)
		).block())
				.isInstanceOf(LearningCandidateRejectedException.class)
				.hasMessage("Latest postmortem review must be APPROVED.");
	}

	private PostmortemDraftRecord draft(PostmortemDraftStatus status) {
		return new PostmortemDraftRecord(
				"draft-1",
				"incident-1",
				status,
				"operator-a",
				"Postmortem draft summary.",
				List.of("timeline"),
				List.of("recommendation"),
				List.of("execution"),
				List.of("verification"),
				List.of("reanalysis"),
				List.of("learning"),
				List.of("open question"),
				Instant.now(),
				Map.of()
		);
	}

	private PostmortemReviewRecord review(PostmortemReviewStatus status) {
		return new PostmortemReviewRecord(
				"review-1",
				"draft-1",
				"incident-1",
				status,
				"operator-a",
				"reviewed",
				"review summary",
				Instant.now(),
				Map.of()
		);
	}
}
