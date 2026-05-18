package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
public class GovernanceKnowledgeUpdateDetailService {

	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionPlanStore promotionPlanStore;
	private final KnowledgePromotionReviewStore promotionReviewStore;
	private final GovernanceDetailTimelineBuilder timelineBuilder;
	private final GovernanceDetailSanitizer sanitizer;
	private final GovernanceDetailComponentLoader componentLoader;
	private final GovernanceDetailMetricsRecorder metricsRecorder;

	public GovernanceKnowledgeUpdateDetailService(
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionPlanStore promotionPlanStore,
			KnowledgePromotionReviewStore promotionReviewStore,
			GovernanceDetailTimelineBuilder timelineBuilder,
			GovernanceDetailSanitizer sanitizer,
			GovernanceDetailComponentLoader componentLoader,
			GovernanceDetailMetricsRecorder metricsRecorder
	) {
		this.knowledgeUpdateStore = knowledgeUpdateStore;
		this.learningCandidateStore = learningCandidateStore;
		this.promotionPlanStore = promotionPlanStore;
		this.promotionReviewStore = promotionReviewStore;
		this.timelineBuilder = timelineBuilder;
		this.sanitizer = sanitizer;
		this.componentLoader = componentLoader;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceKnowledgeUpdateDetailResponse> findByKnowledgeUpdateApplicationId(
			String knowledgeUpdateApplicationId
	) {
		return knowledgeUpdateStore.findById(knowledgeUpdateApplicationId)
				.switchIfEmpty(Mono.defer(() -> {
					metricsRecorder.notFound("knowledgeUpdate");
					return Mono.error(new ResponseStatusException(
							HttpStatus.NOT_FOUND,
							"Governance knowledge update detail not found."
					));
				}))
				.flatMap(update -> {
					List<String> failedComponents =
							java.util.Collections.synchronizedList(new java.util.ArrayList<>());
					AtomicReference<String> degradationReason =
							new AtomicReference<>("none");

					return Mono.zip(
							Mono.just(update),
							componentLoader.optional(
									"learningCandidate",
									learningCandidateStore.findById(update.learningCandidateId()),
									failedComponents,
									degradationReason
							),
							componentLoader.optional(
									"promotionPlan",
									promotionPlanStore.findById(update.promotionPlanId()),
									failedComponents,
									degradationReason
							),
							componentLoader.list(
									"promotionReviews",
									promotionReviewStore.findByLearningCandidateId(update.learningCandidateId())
											.collectList(),
									failedComponents,
									degradationReason
							),
							Mono.just(failedComponents),
							Mono.just(degradationReason)
					);
				})
				.map(tuple -> {
					KnowledgeUpdateApplicationRecord update = tuple.getT1();
					Optional<LearningCandidateRecord> candidate = tuple.getT2();
					Optional<KnowledgePromotionPlanRecord> plan = tuple.getT3();
					List<KnowledgePromotionReviewRecord> promotionReviews =
							sortDesc(tuple.getT4(), KnowledgePromotionReviewRecord::reviewedAt);
					List<String> failedComponents = List.copyOf(tuple.getT5());
					AtomicReference<String> degradationReason = tuple.getT6();

					return new GovernanceKnowledgeUpdateDetailResponse(
							Instant.now(),
							GovernanceDetailType.KNOWLEDGE_UPDATE,
							knowledgeUpdateApplicationId,
							update.incidentId(),
							new GovernanceDetailSummary(
									GovernanceDetailType.KNOWLEDGE_UPDATE,
									update.knowledgeUpdateApplicationId(),
									update.incidentId(),
									sanitizer.safeStatus(update.changeType()),
									"Knowledge update detail " + update.knowledgeUpdateApplicationId(),
									sanitizer.safeText("Governance aggregate detail for knowledge update."),
									update.appliedAt()
							),
							degradation(failedComponents, degradationReason.get()),
							knowledgeUpdateSummary(update),
							candidate.map(this::learningCandidateSummary).orElse(null),
							plan.map(this::promotionPlanSummary).orElse(null),
							promotionReviews.stream().map(this::promotionReviewSummary).toList(),
							sanitizer.safeText(update.knowledgeType()),
							update.knowledgeLayer() == null ? null : update.knowledgeLayer().name(),
							sanitizer.safeText(update.filePath()),
							sanitizer.safeStatus(update.changeType()),
							sanitizer.safeText(update.gitRepository()),
							sanitizer.safeText(update.gitBranch()),
							sanitizer.safeText(update.gitCommitSha()),
							sanitizer.safeText(update.pullRequestReference()),
							sanitizer.safeTexts(update.validationChecks()),
							timelineBuilder.buildLearningTimeline(
									candidate.orElse(null),
									promotionReviews,
									plan.map(List::of).orElseGet(List::of),
									List.of(update)
							)
					);
				})
				.doOnNext(response -> metricsRecorder.success("knowledgeUpdate"))
				.doOnNext(response -> recordDegraded("knowledgeUpdate", response.degradation()))
				.doOnError(ex -> recordFailure("knowledgeUpdate", ex));
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
