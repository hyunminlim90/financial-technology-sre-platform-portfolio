package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DefaultGovernanceTimelineAggregationService
		implements GovernanceTimelineAggregationService {

	private static final String ORDERING = "occurredAt DESC, eventId DESC";

	private final RecommendationRecordStore recommendationRecordStore;
	private final RecommendationApprovalStore recommendationApprovalStore;
	private final ExecutionPlanStore executionPlanStore;
	private final HumanExecutionResultStore humanExecutionResultStore;
	private final VerificationResultStore verificationResultStore;
	private final IncidentLifecycleStore incidentLifecycleStore;
	private final PostmortemDraftStore postmortemDraftStore;
	private final PostmortemReviewStore postmortemReviewStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionReviewStore knowledgePromotionReviewStore;
	private final KnowledgePromotionPlanStore knowledgePromotionPlanStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateApplicationStore;
	private final GovernanceTimelineProjectionMapper projectionMapper;
	private final GovernanceTimelineCursorCodec cursorCodec;
	private final GovernanceTimelineMetricsRecorder metricsRecorder;

	public DefaultGovernanceTimelineAggregationService(
			RecommendationRecordStore recommendationRecordStore,
			RecommendationApprovalStore recommendationApprovalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore humanExecutionResultStore,
			VerificationResultStore verificationResultStore,
			IncidentLifecycleStore incidentLifecycleStore,
			PostmortemDraftStore postmortemDraftStore,
			PostmortemReviewStore postmortemReviewStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionReviewStore knowledgePromotionReviewStore,
			KnowledgePromotionPlanStore knowledgePromotionPlanStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateApplicationStore,
			GovernanceTimelineProjectionMapper projectionMapper,
			GovernanceTimelineCursorCodec cursorCodec,
			GovernanceTimelineMetricsRecorder metricsRecorder
	) {
		this.recommendationRecordStore = recommendationRecordStore;
		this.recommendationApprovalStore = recommendationApprovalStore;
		this.executionPlanStore = executionPlanStore;
		this.humanExecutionResultStore = humanExecutionResultStore;
		this.verificationResultStore = verificationResultStore;
		this.incidentLifecycleStore = incidentLifecycleStore;
		this.postmortemDraftStore = postmortemDraftStore;
		this.postmortemReviewStore = postmortemReviewStore;
		this.learningCandidateStore = learningCandidateStore;
		this.knowledgePromotionReviewStore = knowledgePromotionReviewStore;
		this.knowledgePromotionPlanStore = knowledgePromotionPlanStore;
		this.knowledgeUpdateApplicationStore = knowledgeUpdateApplicationStore;
		this.projectionMapper = projectionMapper;
		this.cursorCodec = cursorCodec;
		this.metricsRecorder = metricsRecorder;
	}

	@Override
	public Mono<GovernanceTimelineAggregationResult> aggregate(
			GovernanceTimelineAggregationRequest request
	) {
		GovernanceTimelineAggregationRequest safeRequest =
				request == null
						? new GovernanceTimelineAggregationRequest(
								new GovernanceTimelineQuery(
										null,
										null,
										GovernanceCursorDirection.NEXT,
										null
								),
								List.of()
						)
						: request;
		GovernanceTimelineQuery safeQuery = safeQuery(safeRequest.query());
		int safeLimit = safeQuery.safeLimit();
		int fetchLimit = Math.min(Math.max(safeLimit * 2, 20), 500);

		return Flux.fromIterable(safeRequest.safeSources())
				.concatMap(source -> load(source, fetchLimit))
				.collectList()
				.map(results -> aggregateResults(results, safeQuery));
	}

	private GovernanceTimelineAggregationResult aggregateResults(
			List<SourceAggregationResult> results,
			GovernanceTimelineQuery query
	) {
		List<String> failedSources = results.stream()
				.map(SourceAggregationResult::failedSource)
				.filter(Objects::nonNull)
				.map(Enum::name)
				.toList();
		List<GovernanceTimelineProjection> projections = results.stream()
				.flatMap(result -> result.projections().stream())
				.filter(projection -> matchesFilters(projection, query.filter()))
				.sorted(projectionComparator())
				.toList();

		Map<String, GovernanceTimelineProjection> deduplicated = new LinkedHashMap<>();
		for (GovernanceTimelineProjection projection : projections) {
			deduplicated.putIfAbsent(projection.event().eventId(), projection);
		}

		GovernanceTimelineCursor cursor = decodeCursor(query.cursor());
		List<GovernanceTimelineProjection> boundaryFiltered = deduplicated.values().stream()
				.filter(projection -> withinCursorBoundary(
						projection,
						cursor,
						query.safeDirection()
				))
				.toList();
		boolean hasMore = boundaryFiltered.size() > query.safeLimit();
		List<GovernanceTimelineProjection> pageProjections = boundaryFiltered.stream()
				.limit(query.safeLimit())
				.toList();
		List<GovernanceDetailTimelineItem> items = pageProjections.stream()
				.map(this::item)
				.toList();
		boolean hasNext = query.safeDirection() == GovernanceCursorDirection.NEXT
				? hasMore
				: cursor != null;
		boolean hasPrevious = query.safeDirection() == GovernanceCursorDirection.PREVIOUS
				? hasMore
				: cursor != null;
		String previousCursor = pageProjections.isEmpty()
				? null
				: cursorOf(pageProjections.get(0));
		String nextCursor = pageProjections.isEmpty()
				? null
				: cursorOf(pageProjections.get(pageProjections.size() - 1));
		GovernanceTimelinePageResponse page = new GovernanceTimelinePageResponse(
				items,
				new GovernanceTimelinePageMetadata(
						nextCursor,
						previousCursor,
						hasNext,
						hasPrevious,
						query.safeLimit(),
						query.safeDirection(),
						ORDERING,
						!failedSources.isEmpty(),
						failedSources
				)
		);
		recordAggregationMetrics(page, failedSources);

		if (failedSources.isEmpty()) {
			return GovernanceTimelineAggregationResult.success(page);
		}
		return GovernanceTimelineAggregationResult.degraded(
				page,
				failedSources,
				"timeline_aggregation_degraded"
		);
	}

	private Mono<SourceAggregationResult> load(
			GovernanceTimelineAggregationSource source,
			int fetchLimit
	) {
		return records(source, fetchLimit)
				.map(projectionMapper::project)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collectList()
				.map(projections -> new SourceAggregationResult(source, projections, null))
				.onErrorResume(ex -> Mono.just(new SourceAggregationResult(
						source,
						List.of(),
						source
				)));
	}

	private Flux<?> records(
			GovernanceTimelineAggregationSource source,
			int fetchLimit
	) {
		return switch (source) {
			case RECOMMENDATION -> recommendationRecordStore.findRecent(fetchLimit);
			case APPROVAL -> recommendationApprovalStore.findRecent(fetchLimit);
			case EXECUTION_PLAN -> executionPlanStore.findRecent(fetchLimit);
			case HUMAN_EXECUTION -> humanExecutionResultStore.findRecent(fetchLimit);
			case VERIFICATION -> verificationResultStore.findRecent(fetchLimit);
			case INCIDENT_LIFECYCLE -> incidentLifecycleStore.findRecent(fetchLimit);
			case POSTMORTEM_DRAFT -> postmortemDraftStore.findRecent(fetchLimit);
			case POSTMORTEM_REVIEW -> postmortemReviewStore.findRecent(fetchLimit);
			case LEARNING_CANDIDATE -> learningCandidateStore.findRecent(fetchLimit);
			case KNOWLEDGE_PROMOTION_REVIEW ->
					knowledgePromotionReviewStore.findRecent(fetchLimit);
			case KNOWLEDGE_PROMOTION_PLAN ->
					knowledgePromotionPlanStore.findRecent(fetchLimit);
			case KNOWLEDGE_UPDATE -> knowledgeUpdateApplicationStore.findRecent(fetchLimit);
		};
	}

	private GovernanceTimelineQuery safeQuery(GovernanceTimelineQuery query) {
		return query == null
				? new GovernanceTimelineQuery(null, null, null, null)
				: query;
	}

	private GovernanceTimelineCursor decodeCursor(String encodedCursor) {
		if (encodedCursor == null || encodedCursor.isBlank()) {
			return null;
		}
		return cursorCodec.decode(encodedCursor);
	}

	private boolean matchesFilters(
			GovernanceTimelineProjection projection,
			GovernanceTimelineFilter filter
	) {
		if (filter == null) {
			return true;
		}
		if (!matchesEventTypes(projection, filter.eventTypes())) {
			return false;
		}
		if (!matchesTimeRange(projection.event().occurredAt(), filter.from(), filter.to())) {
			return false;
		}
		return true;
	}

	private boolean matchesEventTypes(
			GovernanceTimelineProjection projection,
			List<GovernanceTimelineEventType> eventTypes
	) {
		return eventTypes == null
				|| eventTypes.isEmpty()
				|| eventTypes.contains(projection.event().eventType());
	}

	private boolean matchesTimeRange(
			Instant occurredAt,
			Instant from,
			Instant to
	) {
		if (occurredAt == null) {
			return false;
		}
		if (from != null && occurredAt.isBefore(from)) {
			return false;
		}
		if (to != null && occurredAt.isAfter(to)) {
			return false;
		}
		return true;
	}

	private boolean withinCursorBoundary(
			GovernanceTimelineProjection projection,
			GovernanceTimelineCursor cursor,
			GovernanceCursorDirection direction
	) {
		if (cursor == null) {
			return true;
		}

		int occurredAtComparison = compareOccurredAt(
				projection.event().occurredAt(),
				cursor.occurredAt()
		);
		int eventIdComparison = compareEventId(
				projection.event().eventId(),
				cursor.eventId()
		);

		if (direction == GovernanceCursorDirection.PREVIOUS) {
			return occurredAtComparison > 0
					|| (occurredAtComparison == 0 && eventIdComparison > 0);
		}

		return occurredAtComparison < 0
				|| (occurredAtComparison == 0 && eventIdComparison < 0);
	}

	private int compareOccurredAt(Instant left, Instant right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return -1;
		}
		if (right == null) {
			return 1;
		}
		return left.compareTo(right);
	}

	private int compareEventId(String left, String right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return -1;
		}
		if (right == null) {
			return 1;
		}
		return left.compareTo(right);
	}

	private Comparator<GovernanceTimelineProjection> projectionComparator() {
		return Comparator.comparing(
				(GovernanceTimelineProjection projection) -> projection.event().occurredAt(),
				Comparator.nullsLast(Comparator.reverseOrder())
		).thenComparing(
				projection -> projection.event().eventId(),
				Comparator.nullsLast(Comparator.reverseOrder())
		);
	}

	private GovernanceDetailTimelineItem item(
			GovernanceTimelineProjection projection
	) {
		GovernanceTimelineEvent event = projection.event();
		return new GovernanceDetailTimelineItem(
				event.occurredAt(),
				event.eventType().name(),
				projection.sourceId(),
				statusOf(event),
				event.title(),
				event.summary()
		);
	}

	private String statusOf(GovernanceTimelineEvent event) {
		if (event.metadata() != null
				&& event.metadata().attributes() != null
				&& event.metadata().attributes().containsKey("status")) {
			return event.metadata().attributes().get("status");
		}
		return event.severity() == null ? "UNKNOWN" : event.severity().name();
	}

	private String cursorOf(GovernanceTimelineProjection projection) {
		GovernanceTimelineEvent event = projection.event();
		return cursorCodec.encode(new GovernanceTimelineCursor(
				event.occurredAt(),
				event.eventType().name(),
				event.eventId()
		));
	}

	private void recordAggregationMetrics(
			GovernanceTimelinePageResponse page,
			List<String> failedSources
	) {
		GovernanceTimelineResilienceMode mode = failedSources.isEmpty()
				? GovernanceTimelineResilienceMode.STRICT
				: GovernanceTimelineResilienceMode.PARTIAL_DEGRADED;
		metricsRecorder.pageSize(
				mode,
				page == null || page.items() == null ? 0 : page.items().size()
		);
		failedSources.forEach(source -> metricsRecorder.degraded(
				mode,
				"timeline_aggregation_degraded",
				GovernanceTimelineAggregationSource.valueOf(source)
		));
	}

	private record SourceAggregationResult(
			GovernanceTimelineAggregationSource source,
			List<GovernanceTimelineProjection> projections,
			GovernanceTimelineAggregationSource failedSource
	) {
	}
}
