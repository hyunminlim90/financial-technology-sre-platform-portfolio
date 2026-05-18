package com.fintech.sre.agent.recommendation.execution;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.ExecutionMetricsRecorder;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;

import reactor.core.publisher.Mono;

@Service
public class RecommendationExecutionPlanService {

	private final RecommendationRecordStore recommendationStore;
	private final RecommendationApprovalStore approvalStore;
	private final ExecutionPlanStore executionPlanStore;
	private final ExecutionPlanStepMapper stepMapper;
	private final ExecutionPlanIdGenerator idGenerator;
	private final ExecutionMetricsRecorder metricsRecorder;

	public RecommendationExecutionPlanService(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			ExecutionPlanStepMapper stepMapper,
			ExecutionPlanIdGenerator idGenerator,
			ExecutionMetricsRecorder metricsRecorder
	) {
		this.recommendationStore = recommendationStore;
		this.approvalStore = approvalStore;
		this.executionPlanStore = executionPlanStore;
		this.stepMapper = stepMapper;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<ExecutionPlanResponse> createDryRunPlan(
			String recommendationRecordId,
			ExecutionPlanRequest request
	) {
		return validate(request)
				.then(recommendationStore.findById(recommendationRecordId))
				.switchIfEmpty(Mono.error(new ExecutionPlanRejectedException(
						"RECOMMENDATION_RECORD_NOT_FOUND",
						"Recommendation record not found."
				)))
				.flatMap(record ->
						approvalStore.findLatestByRecommendationRecordId(recommendationRecordId)
								.switchIfEmpty(Mono.error(new ExecutionPlanRejectedException(
										"RECOMMENDATION_NOT_APPROVED",
										"Recommendation must be approved before creating execution plan."
								)))
								.flatMap(approval -> {
									if (approval.status() != RecommendationApprovalStatus.APPROVED) {
										return Mono.error(new ExecutionPlanRejectedException(
												"RECOMMENDATION_NOT_APPROVED",
												"Only APPROVED recommendation can create execution plan."
										));
									}

									return savePlan(record, request);
								})
				);
	}

	public Mono<RecommendationExecutionPlan> findById(String executionPlanId) {
		return executionPlanStore.findById(executionPlanId);
	}

	private Mono<Void> validate(ExecutionPlanRequest request) {
		if (request == null) {
			return Mono.error(new ExecutionPlanRejectedException(
					"EXECUTION_PLAN_REQUEST_REQUIRED",
					"Execution plan request is required."
			));
		}

		if (request.operatorId() == null || request.operatorId().isBlank()) {
			return Mono.error(new ExecutionPlanRejectedException(
					"OPERATOR_ID_REQUIRED",
					"operatorId is required."
			));
		}

		if (request.reason() == null || request.reason().isBlank()) {
			return Mono.error(new ExecutionPlanRejectedException(
					"EXECUTION_PLAN_REASON_REQUIRED",
					"reason is required."
			));
		}

		return Mono.empty();
	}

	private Mono<ExecutionPlanResponse> savePlan(
			RecommendationRecord record,
			ExecutionPlanRequest request
	) {
		List<ExecutionPlanStep> steps = stepMapper.toSteps(record);
		List<String> blockedReasons = blockedReasons(record, steps);
		boolean blocked = !blockedReasons.isEmpty();
		boolean requiresFinalApproval = requiresFinalApproval(record);

		RecommendationExecutionPlan plan = new RecommendationExecutionPlan(
				idGenerator.generate(),
				record.recommendationRecordId(),
				record.incidentId(),
				blocked ? ExecutionPlanStatus.BLOCKED : ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				requiresFinalApproval,
				request.operatorId(),
				request.reason(),
				Instant.now(),
				steps,
				blockedReasons
		);

		return executionPlanStore.save(plan)
				.doOnNext(metricsRecorder::recordPlanCreated)
				.map(this::toResponse);
	}

	private List<String> blockedReasons(
			RecommendationRecord record,
			List<ExecutionPlanStep> steps
	) {
		ArrayList<String> reasons = new ArrayList<>();

		if (steps == null || steps.isEmpty()) {
			reasons.add("NO_ACTION_TYPES_AVAILABLE_FOR_EXECUTION_PLAN");
		}

		if (record.forbiddenActionCount() > 0) {
			reasons.add("RECOMMENDATION_HAS_FORBIDDEN_ACTIONS");
		}

		if (record.blockedReasons() != null && !record.blockedReasons().isEmpty()) {
			reasons.addAll(record.blockedReasons());
		}

		return List.copyOf(reasons);
	}

	private boolean requiresFinalApproval(RecommendationRecord record) {
		if (record == null) {
			return true;
		}

		boolean paymentDomain = "payment".equalsIgnoreCase(record.domain())
				|| "payments".equalsIgnoreCase(record.domain());

		boolean highRisk = "CRITICAL".equalsIgnoreCase(record.severity())
				|| "HIGH".equalsIgnoreCase(record.severity());

		return paymentDomain || highRisk;
	}

	private ExecutionPlanResponse toResponse(RecommendationExecutionPlan plan) {
		return new ExecutionPlanResponse(
				plan.executionPlanId(),
				plan.recommendationRecordId(),
				plan.incidentId(),
				plan.status(),
				plan.executable(),
				plan.requiresFinalApproval(),
				plan.steps(),
				plan.blockedReasons()
		);
	}
}
