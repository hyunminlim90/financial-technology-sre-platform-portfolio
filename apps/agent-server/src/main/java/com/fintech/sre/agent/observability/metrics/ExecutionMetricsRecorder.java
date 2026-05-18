package com.fintech.sre.agent.observability.metrics;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;

@Component
public class ExecutionMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public ExecutionMetricsRecorder(GovernanceMetricsRecorder recorder) {
		this.recorder = recorder;
	}

	public void recordPlanCreated(RecommendationExecutionPlan plan) {
		recorder.increment(
				GovernanceMetricName.EXECUTION_PLAN_CREATED,
				Map.of(
						"status", plan.status().name(),
						"executable", String.valueOf(plan.executable()),
						"requiresFinalApproval", String.valueOf(plan.requiresFinalApproval())
				)
		);
	}

	public void recordHumanExecution(HumanExecutionResultRecord record) {
		recorder.increment(
				GovernanceMetricName.HUMAN_EXECUTION_RESULT,
				Map.of(
						"status", record.status().name(),
						"incidentId", safe(record.incidentId())
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
