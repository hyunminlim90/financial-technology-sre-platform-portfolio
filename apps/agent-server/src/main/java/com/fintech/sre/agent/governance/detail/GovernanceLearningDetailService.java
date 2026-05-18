package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;

import reactor.core.publisher.Mono;

@Service
public class GovernanceLearningDetailService {

	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionReviewStore promotionReviewStore;
	private final KnowledgePromotionPlanStore promotionPlanStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;
	private final GovernanceDetailTimelineBuilder timelineBuilder;
	private final GovernanceDetailSanitizer sanitizer;
	private final GovernanceDetailComponentLoader componentLoader;
	private final GovernanceDetailMetricsRecorder metricsRecorder;

	public GovernanceLearningDetailService(
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionReviewStore promotionReviewStore,
			KnowledgePromotionPlanStore promotionPlanStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceDetailTimelineBuilder timelineBuilder,
			GovernanceDetailSanitizer sanitizer,
			GovernanceDetailComponentLoader componentLoader,
			GovernanceDetailMetricsRecorder metricsRecorder
	) {
		this.learningCandidateStore = learningCandidateStore;
		this.promotionReviewStore = promotionReviewStore;
		this.promotionPlanStore = promotionPlanStore;
		this.knowledgeUpdateStore = knowledgeUpdateStore;
		this.timelineBuilder = timelineBuilder;
		this.sanitizer = sanitizer;
		this.componentLoader = componentLoader;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceLearningDetailResponse> findByLearningCandidateId(
			String learningCandidateId
	) {
		return learningCandidateStore.findById(learningCandidateId)
				.switchIfEmpty(Mono.defer(() -> {
					metricsRecorder.notFound("learningCandidate");
					return Mono.error(new ResponseStatusException(
							HttpStatus.NOT_FOUND,
							"Governance learning detail not found."
					));
				}))
				.flatMap(candidate -> {
					List<String> failedComponents =
							java.util.Collections.synchronizedList(new java.util.ArrayList<>());
					AtomicReference<String> degradationReason =
							new AtomicReference<>("none");

					return Mono.zip(
							Mono.just(candidate),
							componentLoader.list(
									"promotionReviews",
									promotionReviewStore.findByLearningCandidateId(learningCandidateId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							componentLoader.list(
									"promotionPlans",
									promotionPlanStore.findByLearningCandidateId(learningCandidateId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							componentLoader.list(
									"knowledgeUpdates",
									knowledgeUpdateStore.findByLearningCandidateId(learningCandidateId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							Mono.just(failedComponents),
							Mono.just(degradationReason)
					);
				})
				.map(tuple -> {
					LearningCandidateRecord candidate = tuple.getT1();
					List<KnowledgePromotionReviewRecord> promotionReviews =
							sortDesc(tuple.getT2(), KnowledgePromotionReviewRecord::reviewedAt);
					List<KnowledgePromotionPlanRecord> promotionPlans =
							sortDesc(tuple.getT3(), KnowledgePromotionPlanRecord::createdAt);
					List<KnowledgeUpdateApplicationRecord> knowledgeUpdates =
							sortDesc(tuple.getT4(), KnowledgeUpdateApplicationRecord::appliedAt);
					List<String> failedComponents = List.copyOf(tuple.getT5());
					AtomicReference<String> degradationReason = tuple.getT6();

					return new GovernanceLearningDetailResponse(
							Instant.now(),
							GovernanceDetailType.LEARNING,
							learningCandidateId,
							candidate.incidentId(),
							degradation(failedComponents, degradationReason.get()),
							learningCandidateSummary(candidate),
							promotionReviews.stream().map(this::promotionReviewSummary).toList(),
							promotionPlans.stream().map(this::promotionPlanSummary).toList(),
							knowledgeUpdates.stream().map(this::knowledgeUpdateSummary).toList(),
							timelineBuilder.buildLearningTimeline(
									candidate,
									promotionReviews,
									promotionPlans,
									knowledgeUpdates
							)
					);
				})
				.doOnNext(response -> metricsRecorder.success("learningCandidate"))
				.doOnNext(response -> recordDegraded("learningCandidate", response.degradation()))
				.doOnError(ex -> recordFailure("learningCandidate", ex));
	}

	private GovernanceDetailSummary learningCandidateSummary(LearningCandidateRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.LEARNING,
				record.learningCandidateId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				sanitizer.safeText("Learning candidate " + record.learningCandidateId()),
				sanitizer.safeText(record.summary()),
				record.createdAt()
		);
	}

	private GovernanceDetailSummary promotionReviewSummary(KnowledgePromotionReviewRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.LEARNING,
				record.promotionReviewId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Promotion review " + record.promotionReviewId(),
				sanitizer.safeText(record.reviewSummary()),
				record.reviewedAt()
		);
	}

	private GovernanceDetailSummary promotionPlanSummary(KnowledgePromotionPlanRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.LEARNING,
				record.promotionPlanId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Promotion plan " + record.promotionPlanId(),
				sanitizer.safeText(record.summary()),
				record.createdAt()
		);
	}

	private GovernanceDetailSummary knowledgeUpdateSummary(KnowledgeUpdateApplicationRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.KNOWLEDGE_UPDATE,
				record.knowledgeUpdateApplicationId(),
				record.incidentId(),
				sanitizer.safeStatus(record.changeType()),
				"Knowledge update " + record.knowledgeUpdateApplicationId(),
				sanitizer.safeText(record.filePath()),
				record.appliedAt()
		);
	}

	private <T> List<T> sortDesc(List<T> records, java.util.function.Function<T, Instant> timeExtractor) {
		return records.stream()
				.sorted(Comparator.comparing(timeExtractor, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
				.toList();
	}

	private void recordFailure(String detailType, Throwable ex) {
		if (ex instanceof ResponseStatusException status
				&& status.getStatusCode() == HttpStatus.NOT_FOUND) {
			return;
		}
		metricsRecorder.failure(detailType);
	}

	private void recordDegraded(
			String detailType,
			GovernanceDetailDegradation degradation
	) {
		if (degradation == null || !degradation.degraded()) {
			return;
		}
		degradation.failedComponents().forEach(component ->
				metricsRecorder.degraded(
						detailType,
						degradation.reason(),
						component
				));
	}

	private GovernanceDetailDegradation degradation(
			List<String> failedComponents,
			String reason
	) {
		return failedComponents == null || failedComponents.isEmpty()
				? GovernanceDetailDegradation.none()
				: GovernanceDetailDegradation.partial(failedComponents, reason);
	}
}
