package com.fintech.sre.agent.learning.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;

class KnowledgePromotionReviewServiceTest {

	@Test
	void shouldApproveReviewRequiredLearningCandidate() {
		InMemoryLearningCandidateStore candidateStore = new InMemoryLearningCandidateStore();
		candidateStore.save(candidate(LearningCandidateStatus.REVIEW_REQUIRED)).block();

		KnowledgePromotionReviewService service = new KnowledgePromotionReviewService(
				candidateStore,
				new InMemoryKnowledgePromotionReviewStore(),
				new KnowledgePromotionReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		KnowledgePromotionReviewResponse response = service.review(
				"candidate-1",
				new KnowledgePromotionReviewRequest(
						KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
						"operator-a",
						"reviewed by platform team",
						"Approved for promotion planning.",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.status())
				.isEqualTo(KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION);
	}

	@Test
	void shouldFilterSensitiveMetadata() {
		InMemoryLearningCandidateStore candidateStore = new InMemoryLearningCandidateStore();
		candidateStore.save(candidate(LearningCandidateStatus.REVIEW_REQUIRED)).block();
		InMemoryKnowledgePromotionReviewStore reviewStore = new InMemoryKnowledgePromotionReviewStore();

		KnowledgePromotionReviewService service = new KnowledgePromotionReviewService(
				candidateStore,
				reviewStore,
				new KnowledgePromotionReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		KnowledgePromotionReviewResponse response = service.review(
				"candidate-1",
				new KnowledgePromotionReviewRequest(
						KnowledgePromotionReviewStatus.REJECTED,
						"operator-a",
						"not ready",
						"Reject until scope is reduced.",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		KnowledgePromotionReviewRecord record =
				reviewStore.findLatestByLearningCandidateId(response.learningCandidateId()).block();

		assertThat(record.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload");
	}

	@Test
	void shouldRejectWhenCandidateNotReviewRequired() {
		InMemoryLearningCandidateStore candidateStore = new InMemoryLearningCandidateStore();
		candidateStore.save(candidate(LearningCandidateStatus.APPROVED)).block();

		KnowledgePromotionReviewService service = new KnowledgePromotionReviewService(
				candidateStore,
				new InMemoryKnowledgePromotionReviewStore(),
				new KnowledgePromotionReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		assertThatThrownBy(() -> service.review(
				"candidate-1",
				new KnowledgePromotionReviewRequest(
						KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
						"operator-a",
						"reason",
						"summary",
						Map.of()
				)
		).block())
				.isInstanceOf(KnowledgePromotionReviewRejectedException.class)
				.hasMessage("Only REVIEW_REQUIRED learning candidates can be reviewed for promotion.");
	}

	@Test
	void shouldRequireReviewerAndSummary() {
		KnowledgePromotionReviewService service = new KnowledgePromotionReviewService(
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				new KnowledgePromotionReviewIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		assertThatThrownBy(() -> service.review(
				"candidate-1",
				new KnowledgePromotionReviewRequest(
						KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
						"",
						"reason",
						"",
						Map.of()
				)
		).block())
				.isInstanceOf(KnowledgePromotionReviewRejectedException.class)
				.hasMessage("reviewedBy is required.");
	}

	private LearningCandidateRecord candidate(LearningCandidateStatus status) {
		return new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				status,
				"operator-a",
				"Promote reviewed candidate.",
				List.of("Add verification checklist."),
				Instant.now(),
				Map.of("team", "sre")
		);
	}
}
