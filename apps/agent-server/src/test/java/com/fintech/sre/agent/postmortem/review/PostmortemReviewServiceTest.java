package com.fintech.sre.agent.postmortem.review;

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

class PostmortemReviewServiceTest {

	@Test
	void shouldSaveReviewAndUpdateDraftStatus() {
		InMemoryPostmortemDraftStore draftStore = new InMemoryPostmortemDraftStore();
		draftStore.save(draft("draft-1", PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED)).block();

		PostmortemReviewService service = new PostmortemReviewService(
				new InMemoryPostmortemReviewStore(),
				draftStore,
				new PostmortemReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		PostmortemReviewResponse response = service.review(
				"draft-1",
				new PostmortemReviewRequest(
						PostmortemReviewStatus.APPROVED,
						"operator-a",
						"validated by team",
						"Approved after human validation.",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.status())
				.isEqualTo(PostmortemReviewStatus.APPROVED);

		PostmortemDraftRecord updatedDraft = draftStore.findById("draft-1").block();
		assertThat(updatedDraft.status())
				.isEqualTo(PostmortemDraftStatus.APPROVED);
		assertThat(updatedDraft.metadata()).containsKey("team");
		assertThat(updatedDraft.metadata()).doesNotContainKey("paymentPayload");
	}

	@Test
	void shouldRequireReviewerAndSummary() {
		PostmortemReviewService service = new PostmortemReviewService(
				new InMemoryPostmortemReviewStore(),
				new InMemoryPostmortemDraftStore(),
				new PostmortemReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		assertThatThrownBy(() -> service.review(
				"draft-1",
				new PostmortemReviewRequest(
						PostmortemReviewStatus.APPROVED,
						"",
						"reason",
						"",
						Map.of()
				)
		).block())
				.isInstanceOf(PostmortemReviewRejectedException.class)
				.hasMessage("reviewedBy is required.");
	}

	@Test
	void shouldSetNeedsRevisionBackToHumanReviewRequired() {
		InMemoryPostmortemDraftStore draftStore = new InMemoryPostmortemDraftStore();
		draftStore.save(draft("draft-2", PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED)).block();

		PostmortemReviewService service = new PostmortemReviewService(
				new InMemoryPostmortemReviewStore(),
				draftStore,
				new PostmortemReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		service.review(
				"draft-2",
				new PostmortemReviewRequest(
						PostmortemReviewStatus.NEEDS_REVISION,
						"operator-a",
						"needs edits",
						"Needs revision before approval.",
						Map.of("team", "sre")
				)
		).block();

		assertThat(draftStore.findById("draft-2").block().status())
				.isEqualTo(PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED);
	}

	private PostmortemDraftRecord draft(String id, PostmortemDraftStatus status) {
		return new PostmortemDraftRecord(
				id,
				"incident-1",
				status,
				"operator-a",
				"Postmortem draft for incident incident-1. This is a human-review draft and does not assert root cause certainty.",
				List.of("timeline"),
				List.of("recommendation"),
				List.of("execution"),
				List.of("verification"),
				List.of("reanalysis"),
				List.of("learning"),
				List.of("open question"),
				Instant.now(),
				Map.of("team", "sre")
		);
	}
}
