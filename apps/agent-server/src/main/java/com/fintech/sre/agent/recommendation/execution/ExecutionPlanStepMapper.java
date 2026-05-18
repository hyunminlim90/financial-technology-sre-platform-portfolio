package com.fintech.sre.agent.recommendation.execution;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

@Component
public class ExecutionPlanStepMapper {

	public List<ExecutionPlanStep> toSteps(RecommendationRecord record) {
		if (record == null || record.actionTypes() == null || record.actionTypes().isEmpty()) {
			return List.of();
		}

		return record.actionTypes().stream()
				.distinct()
				.map(actionType -> new ExecutionPlanStep(
						actionType,
						record.service(),
						"UNKNOWN",
						record.severity(),
						"UNKNOWN",
						true,
						true,
						hasNoBlockedReason(record, "ROLLBACK"),
						true,
						hasNoBlockedReason(record, "VERIFICATION"),
						Map.of(
								"source", "recommendation-record",
								"dryRunOnly", "true"
						)
				))
				.toList();
	}

	private boolean hasNoBlockedReason(
			RecommendationRecord record,
			String keyword
	) {
		if (record.blockedReasons() == null || record.blockedReasons().isEmpty()) {
			return true;
		}

		String upperKeyword = keyword.toUpperCase();

		return record.blockedReasons().stream()
				.noneMatch(reason -> reason != null
						&& reason.toUpperCase().contains(upperKeyword));
	}
}
