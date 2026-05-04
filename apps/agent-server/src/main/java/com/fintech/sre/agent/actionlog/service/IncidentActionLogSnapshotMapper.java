package com.fintech.sre.agent.actionlog.service;

import java.util.List;

import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.RollbackRecordEntity;
import com.fintech.sre.agent.actionlog.entity.VerificationResultEntity;
import com.fintech.sre.agent.actionlog.model.ExecutedActionLog;
import com.fintech.sre.agent.actionlog.model.IncidentActionLogSnapshot;
import com.fintech.sre.agent.actionlog.model.RecommendationLog;
import com.fintech.sre.agent.actionlog.model.RollbackLog;
import com.fintech.sre.agent.actionlog.model.VerificationLog;

public final class IncidentActionLogSnapshotMapper {

	private IncidentActionLogSnapshotMapper() {
	}

	public static IncidentActionLogSnapshot map(
			String incidentId,
			List<RecommendationLog> recommendations,
			List<ExecutedActionEntity> executedActions,
			List<VerificationResultEntity> verifications,
			List<RollbackRecordEntity> rollbacks
	) {
		List<VerificationLog> verificationLogs = verifications.stream()
				.map(verification -> new VerificationLog(
						verification.executedActionId(),
						verification.metricName(),
						verification.query(),
						verification.beforeValue(),
						verification.afterValue(),
						verification.expectedCondition(),
						verification.status(),
						verification.checkedAt()
				))
				.toList();

		List<RollbackLog> rollbackLogs = rollbacks.stream()
				.map(rollback -> new RollbackLog(
						rollback.executedActionId(),
						rollback.rollbackAction(),
						rollback.rollbackReason(),
						rollback.rollbackBy(),
						rollback.rollbackAt(),
						rollback.verificationStatus()
				))
				.toList();

		List<ExecutedActionLog> executedActionLogs = executedActions.stream()
				.map(action -> new ExecutedActionLog(
						action.id(),
						action.recommendationId(),
						action.action(),
						action.executedBy(),
						action.executedAt(),
						action.executionMethod(),
						action.executionDetail(),
						action.expectedEffect(),
						action.actualEffect(),
						action.rollbackPlan(),
						action.rollbackExecuted(),
						verificationLogs.stream()
								.filter(verification -> verification.executedActionId().equals(action.id()))
								.toList(),
						rollbackLogs.stream()
								.filter(rollback -> rollback.executedActionId().equals(action.id()))
								.toList()
				))
				.toList();

		return new IncidentActionLogSnapshot(
				incidentId,
				recommendations,
				executedActionLogs,
				verificationLogs,
				rollbackLogs
		);
	}
}
