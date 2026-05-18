package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
public class GovernanceRecommendationDetailService {

	private final RecommendationRecordStore recommendationStore;
	private final RecommendationApprovalStore approvalStore;
	private final ExecutionPlanStore executionPlanStore;
	private final HumanExecutionResultStore executionResultStore;
	private final VerificationResultStore verificationStore;
	private final GovernanceDetailTimelineBuilder timelineBuilder;
	private final GovernanceDetailSanitizer sanitizer;
	private final GovernanceDetailComponentLoader componentLoader;
	private final GovernanceDetailMetricsRecorder metricsRecorder;

	public GovernanceRecommendationDetailService(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationStore,
			GovernanceDetailTimelineBuilder timelineBuilder,
			GovernanceDetailSanitizer sanitizer,
			GovernanceDetailComponentLoader componentLoader,
			GovernanceDetailMetricsRecorder metricsRecorder
	) {
		this.recommendationStore = recommendationStore;
		this.approvalStore = approvalStore;
		this.executionPlanStore = executionPlanStore;
		this.executionResultStore = executionResultStore;
		this.verificationStore = verificationStore;
		this.timelineBuilder = timelineBuilder;
		this.sanitizer = sanitizer;
		this.componentLoader = componentLoader;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceRecommendationDetailResponse> findByRecommendationRecordId(
			String recommendationRecordId
	) {
		return recommendationStore.findById(recommendationRecordId)
				.switchIfEmpty(Mono.defer(() -> {
					metricsRecorder.notFound("recommendation");
					return Mono.error(new ResponseStatusException(
							HttpStatus.NOT_FOUND,
							"Governance recommendation detail not found."
					));
				}))
				.flatMap(recommendation -> {
					List<String> failedComponents =
							java.util.Collections.synchronizedList(new java.util.ArrayList<>());
					AtomicReference<String> degradationReason =
							new AtomicReference<>("none");

					return Mono.zip(
							Mono.just(recommendation),
							componentLoader.list(
									"approvals",
									approvalStore.findByRecommendationRecordId(recommendationRecordId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							componentLoader.list(
									"executionPlans",
									executionPlanStore.findByRecommendationRecordId(recommendationRecordId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							componentLoader.list(
									"humanExecutionResults",
									executionResultStore.findByRecommendationRecordId(recommendationRecordId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							componentLoader.list(
									"verifications",
									verificationStore.findByRecommendationRecordId(recommendationRecordId)
											.collectList(),
									failedComponents,
									degradationReason
							),
							Mono.just(failedComponents),
							Mono.just(degradationReason)
					);
				})
				.map(tuple -> {
					RecommendationRecord recommendation = tuple.getT1();
					List<RecommendationApprovalRecord> approvals = sortDesc(tuple.getT2(), RecommendationApprovalRecord::decidedAt);
					List<RecommendationExecutionPlan> executionPlans = sortDesc(tuple.getT3(), RecommendationExecutionPlan::createdAt);
					List<HumanExecutionResultRecord> humanExecutionResults = sortDesc(tuple.getT4(), HumanExecutionResultRecord::recordedAt);
					List<VerificationResultRecord> verifications = sortDesc(tuple.getT5(), VerificationResultRecord::verifiedAt);
					List<String> failedComponents = List.copyOf(tuple.getT6());
					AtomicReference<String> degradationReason = tuple.getT7();

					GovernanceDetailSummary recommendationSummary = recommendationSummary(recommendation);

					return new GovernanceRecommendationDetailResponse(
							Instant.now(),
							GovernanceDetailType.RECOMMENDATION,
							recommendationRecordId,
							recommendation.incidentId(),
							new GovernanceDetailSummary(
									GovernanceDetailType.RECOMMENDATION,
									recommendationRecordId,
									recommendation.incidentId(),
									sanitizer.safeStatus(recommendation.policyDecision()),
									"Recommendation detail " + recommendationRecordId,
									sanitizer.safeText("Governance aggregate detail for recommendation."),
									recommendation.generatedAt()
							),
							degradation(failedComponents, degradationReason.get()),
							recommendationSummary,
							approvals.stream().map(this::approvalSummary).toList(),
							executionPlans.stream().map(this::executionPlanSummary).toList(),
							humanExecutionResults.stream().map(this::humanExecutionSummary).toList(),
							verifications.stream().map(this::verificationSummary).toList(),
							timelineBuilder.buildRecommendationTimeline(
									recommendation,
									approvals,
									executionPlans,
									humanExecutionResults,
									verifications
							)
					);
				})
				.doOnNext(response -> metricsRecorder.success("recommendation"))
				.doOnNext(response -> recordDegraded("recommendation", response.degradation()))
				.doOnError(ex -> recordFailure("recommendation", ex));
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
