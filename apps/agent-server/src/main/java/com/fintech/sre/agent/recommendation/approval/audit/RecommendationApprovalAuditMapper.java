package com.fintech.sre.agent.recommendation.approval.audit;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;

@Component
public class RecommendationApprovalAuditMapper {

	private final RecommendationApprovalAuditIdGenerator idGenerator;

	public RecommendationApprovalAuditMapper(
			RecommendationApprovalAuditIdGenerator idGenerator
	) {
		this.idGenerator = idGenerator;
	}

	public RecommendationApprovalAuditLog toAuditLog(
			RecommendationApprovalRecord record
	) {
		return new RecommendationApprovalAuditLog(
				idGenerator.generate(),
				record.recommendationRecordId(),
				record.incidentId(),
				record.status(),
				record.operatorId(),
				record.reason(),
				record.decidedAt(),
				sanitizeMetadata(record.metadata())
		);
	}

	private Map<String, String> sanitizeMetadata(
			Map<String, String> metadata
	) {
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
				&& !lower.contains("prompt")
				&& !lower.contains("payment");
	}
}
