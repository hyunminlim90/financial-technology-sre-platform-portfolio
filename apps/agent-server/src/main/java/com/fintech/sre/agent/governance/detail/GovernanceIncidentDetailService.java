package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
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

import reactor.core.publisher.Mono;

@Service
public class GovernanceIncidentDetailService {

	private final IncidentLifecycleStore incidentLifecycleStore;
	private final RecommendationRecordStore recommendationStore;
	private final RecommendationApprovalStore approvalStore;
	private final ExecutionPlanStore executionPlanStore;
	private final HumanExecutionResultStore executionResultStore;
	private final VerificationResultStore verificationStore;
	private final PostmortemDraftStore postmortemDraftStore;
	private final PostmortemReviewStore postmortemReviewStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;
	private final GovernanceDetailTimelineBuilder timelineBuilder;
	private final GovernanceDetailSanitizer sanitizer;
	private final GovernanceDetailComponentLoader componentLoader;
	private final GovernanceDetailMetricsRecorder metricsRecorder;

	public GovernanceIncidentDetailService(
			IncidentLifecycleStore incidentLifecycleStore,
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationStore,
			PostmortemDraftStore postmortemDraftStore,
			PostmortemReviewStore postmortemReviewStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceDetailTimelineBuilder timelineBuilder,
			GovernanceDetailSanitizer sanitizer,
			GovernanceDetailComponentLoader componentLoader,
			GovernanceDetailMetricsRecorder metricsRecorder
	) {
		this.incidentLifecycleStore = incidentLifecycleStore;
		this.recommendationStore = recommendationStore;
		this.approvalStore = approvalStore;
		this.executionPlanStore = executionPlanStore;
		this.executionResultStore = executionResultStore;
		this.verificationStore = verificationStore;
		this.postmortemDraftStore = postmortemDraftStore;
		this.postmortemReviewStore = postmortemReviewStore;
		this.learningCandidateStore = learningCandidateStore;
		this.knowledgeUpdateStore = knowledgeUpdateStore;
		this.timelineBuilder = timelineBuilder;
		this.sanitizer = sanitizer;
		this.componentLoader = componentLoader;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceIncidentDetailResponse> findByIncidentId(String incidentId) {
		List<String> failedComponents = Collections.synchronizedList(new ArrayList<>());
		AtomicReference<String> degradationReason = new AtomicReference<>("none");

		return Mono.zip(objects -> objects,
				componentLoader.list(
						"lifecycle",
						incidentLifecycleStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"recommendations",
						recommendationStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"approvals",
						approvalStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"executionPlans",
						executionPlanStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"humanExecutionResults",
						executionResultStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"verifications",
						verificationStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"postmortemDrafts",
						postmortemDraftStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"postmortemReviews",
						postmortemReviewStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"learningCandidates",
						learningCandidateStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				),
				componentLoader.list(
						"knowledgeUpdates",
						knowledgeUpdateStore.findByIncidentId(incidentId).collectList(),
						failedComponents,
						degradationReason
				)
		).flatMap(objects -> {
			List<IncidentLifecycleRecord> lifecycles =
					sortDesc(cast(objects[0]), IncidentLifecycleRecord::transitionedAt);
			List<RecommendationRecord> recommendations =
					sortDesc(cast(objects[1]), RecommendationRecord::generatedAt);
			List<RecommendationApprovalRecord> approvals =
					sortDesc(cast(objects[2]), RecommendationApprovalRecord::decidedAt);
			List<RecommendationExecutionPlan> executionPlans =
					sortDesc(cast(objects[3]), RecommendationExecutionPlan::createdAt);
			List<HumanExecutionResultRecord> humanExecutionResults =
					sortDesc(cast(objects[4]), HumanExecutionResultRecord::recordedAt);
			List<VerificationResultRecord> verifications =
					sortDesc(cast(objects[5]), VerificationResultRecord::verifiedAt);
			List<PostmortemDraftRecord> postmortemDrafts =
					sortDesc(cast(objects[6]), PostmortemDraftRecord::createdAt);
			List<PostmortemReviewRecord> postmortemReviews =
					sortDesc(cast(objects[7]), PostmortemReviewRecord::reviewedAt);
			List<LearningCandidateRecord> learningCandidates =
					sortDesc(cast(objects[8]), LearningCandidateRecord::createdAt);
			List<KnowledgeUpdateApplicationRecord> knowledgeUpdates =
					sortDesc(cast(objects[9]), KnowledgeUpdateApplicationRecord::appliedAt);

			if (allEmpty(
					lifecycles,
					recommendations,
					approvals,
					executionPlans,
					humanExecutionResults,
					verifications,
					postmortemDrafts,
					postmortemReviews,
					learningCandidates,
					knowledgeUpdates
			)) {
				metricsRecorder.notFound("incident");
				return Mono.error(new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Governance incident detail not found."
				));
			}

			String currentStatus = lifecycles.stream()
					.max(Comparator.comparing(IncidentLifecycleRecord::transitionedAt))
					.map(record -> sanitizer.safeStatus(record.currentStatus()))
					.orElse("UNKNOWN");

			GovernanceDetailSummary summary = new GovernanceDetailSummary(
					GovernanceDetailType.INCIDENT,
					incidentId,
					incidentId,
					currentStatus,
					"Incident " + incidentId,
					sanitizer.safeText("Governance aggregate detail for incident."),
					lifecycles.stream()
							.max(Comparator.comparing(IncidentLifecycleRecord::transitionedAt))
							.map(IncidentLifecycleRecord::transitionedAt)
							.orElseGet(() -> firstOccurredAt(
									recommendations,
									RecommendationRecord::generatedAt
							))
			);

			return Mono.just(new GovernanceIncidentDetailResponse(
					Instant.now(),
					GovernanceDetailType.INCIDENT,
					incidentId,
					summary,
					currentStatus,
					degradation(List.copyOf(failedComponents), degradationReason.get()),
					timelineBuilder.buildIncidentTimeline(
							lifecycles,
							recommendations,
							approvals,
							executionPlans,
							humanExecutionResults,
							verifications,
							postmortemDrafts,
							postmortemReviews,
							learningCandidates,
							knowledgeUpdates
					),
					recommendations.stream().map(this::recommendationSummary).toList(),
					approvals.stream().map(this::approvalSummary).toList(),
					executionPlans.stream().map(this::executionPlanSummary).toList(),
					humanExecutionResults.stream().map(this::humanExecutionSummary).toList(),
					verifications.stream().map(this::verificationSummary).toList(),
					postmortemDrafts.stream().map(this::postmortemDraftSummary).toList(),
					postmortemReviews.stream().map(this::postmortemReviewSummary).toList(),
					learningCandidates.stream().map(this::learningCandidateSummary).toList(),
					knowledgeUpdates.stream().map(this::knowledgeUpdateSummary).toList()
			));
		})
				.doOnNext(response -> metricsRecorder.success("incident"))
				.doOnNext(response -> recordDegraded("incident", response.degradation()))
				.doOnError(ex -> recordFailure("incident", ex));
	}

	private GovernanceDetailSummary recommendationSummary(RecommendationRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.RECOMMENDATION,
				record.recommendationRecordId(),
				record.incidentId(),
				sanitizer.safeStatus(record.policyDecision()),
				sanitizer.safeText("Recommendation " + record.recommendationRecordId()),
				sanitizer.safeText(record.service() + " / " + record.domain()),
				record.generatedAt()
		);
	}

	private GovernanceDetailSummary approvalSummary(RecommendationApprovalRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.RECOMMENDATION,
				record.approvalId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Approval " + record.approvalId(),
				sanitizer.safeText(record.reason()),
				record.decidedAt()
		);
	}

	private GovernanceDetailSummary executionPlanSummary(RecommendationExecutionPlan record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.RECOMMENDATION,
				record.executionPlanId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Execution plan " + record.executionPlanId(),
				sanitizer.safeText(record.reason()),
				record.createdAt()
		);
	}

	private GovernanceDetailSummary humanExecutionSummary(HumanExecutionResultRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.RECOMMENDATION,
				record.executionResultId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Human execution " + record.executionResultId(),
				sanitizer.safeText(record.summary()),
				record.recordedAt()
		);
	}

	private GovernanceDetailSummary verificationSummary(VerificationResultRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.RECOMMENDATION,
				record.verificationResultId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Verification " + record.verificationResultId(),
				sanitizer.safeText(record.summary()),
				record.verifiedAt()
		);
	}

	private GovernanceDetailSummary postmortemDraftSummary(PostmortemDraftRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.INCIDENT,
				record.postmortemDraftId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Postmortem draft " + record.postmortemDraftId(),
				sanitizer.safeText(record.summary()),
				record.createdAt()
		);
	}

	private GovernanceDetailSummary postmortemReviewSummary(PostmortemReviewRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.INCIDENT,
				record.postmortemReviewId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Postmortem review " + record.postmortemReviewId(),
				sanitizer.safeText(record.reviewSummary()),
				record.reviewedAt()
		);
	}

	private GovernanceDetailSummary learningCandidateSummary(LearningCandidateRecord record) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.LEARNING,
				record.learningCandidateId(),
				record.incidentId(),
				sanitizer.safeStatus(record.status()),
				"Learning candidate " + record.learningCandidateId(),
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

	@SafeVarargs
	private final boolean allEmpty(List<?>... lists) {
		for (List<?> list : lists) {
			if (list != null && !list.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private <T> List<T> sortDesc(List<T> records, java.util.function.Function<T, Instant> timeExtractor) {
		return records.stream()
				.sorted(Comparator.comparing(timeExtractor, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
				.toList();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> cast(Object value) {
		return (List<T>) value;
	}

	private <T> Instant firstOccurredAt(List<T> records, java.util.function.Function<T, Instant> timeExtractor) {
		return records.stream()
				.map(timeExtractor)
				.filter(java.util.Objects::nonNull)
				.findFirst()
				.orElse(Instant.now());
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
