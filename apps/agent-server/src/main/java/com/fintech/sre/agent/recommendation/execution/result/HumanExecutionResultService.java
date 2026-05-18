package com.fintech.sre.agent.recommendation.execution.result;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.ExecutionMetricsRecorder;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;

import reactor.core.publisher.Mono;

@Service
public class HumanExecutionResultService {

	private final ExecutionPlanStore executionPlanStore;
	private final HumanExecutionResultStore resultStore;
	private final HumanExecutionResultIdGenerator idGenerator;
	private final ExecutionMetricsRecorder metricsRecorder;

	public HumanExecutionResultService(
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore resultStore,
			HumanExecutionResultIdGenerator idGenerator,
			ExecutionMetricsRecorder metricsRecorder
	) {
		this.executionPlanStore = executionPlanStore;
		this.resultStore = resultStore;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<HumanExecutionResultResponse> record(
			String executionPlanId,
			HumanExecutionResultRequest request
	) {
		return validate(request)
				.then(executionPlanStore.findById(executionPlanId))
				.switchIfEmpty(Mono.error(new HumanExecutionResultRejectedException(
						"EXECUTION_PLAN_NOT_FOUND",
						"Execution plan not found."
				)))
				.flatMap(plan -> {
					if (plan.status() != ExecutionPlanStatus.DRY_RUN_PLAN_CREATED) {
						return Mono.error(new HumanExecutionResultRejectedException(
								"EXECUTION_PLAN_NOT_RECORDABLE",
								"Only DRY_RUN_PLAN_CREATED plans can accept human execution result."
						));
					}

					return save(plan, request);
				});
	}

	public Mono<HumanExecutionResultRecord> findById(String executionResultId) {
		return resultStore.findById(executionResultId);
	}

	private Mono<Void> validate(HumanExecutionResultRequest request) {
		if (request == null) {
			return Mono.error(new HumanExecutionResultRejectedException(
					"EXECUTION_RESULT_REQUEST_REQUIRED",
					"Execution result request is required."
			));
		}

		if (request.status() == null) {
			return Mono.error(new HumanExecutionResultRejectedException(
					"EXECUTION_RESULT_STATUS_REQUIRED",
					"status is required."
			));
		}

		if (request.operatorId() == null || request.operatorId().isBlank()) {
			return Mono.error(new HumanExecutionResultRejectedException(
					"OPERATOR_ID_REQUIRED",
					"operatorId is required."
			));
		}

		if (request.summary() == null || request.summary().isBlank()) {
			return Mono.error(new HumanExecutionResultRejectedException(
					"EXECUTION_RESULT_SUMMARY_REQUIRED",
					"summary is required."
			));
		}

		return Mono.empty();
	}

	private Mono<HumanExecutionResultResponse> save(
			RecommendationExecutionPlan plan,
			HumanExecutionResultRequest request
	) {
		HumanExecutionResultRecord record = new HumanExecutionResultRecord(
				idGenerator.generate(),
				plan.executionPlanId(),
				plan.recommendationRecordId(),
				plan.incidentId(),
				request.status(),
				request.operatorId(),
				request.summary(),
				request.startedAt(),
				request.finishedAt(),
				Instant.now(),
				sanitizeMetadata(request.metadata())
		);

		return resultStore.save(record)
				.doOnNext(metricsRecorder::recordHumanExecution)
				.map(this::toResponse);
	}

	private HumanExecutionResultResponse toResponse(HumanExecutionResultRecord record) {
		return new HumanExecutionResultResponse(
				record.executionResultId(),
				record.executionPlanId(),
				record.recommendationRecordId(),
				record.incidentId(),
				record.status(),
				record.operatorId(),
				record.summary()
		);
	}

	private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
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
				&& !lower.contains("password")
				&& !lower.contains("commandoutput")
				&& !lower.contains("rawlog")
				&& !lower.contains("payment");
	}
}
