package com.fintech.sre.agent.recommendation.approval;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.ApprovalMetricsRecorder;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditLogger;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditMapper;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;

import reactor.core.publisher.Mono;

@Service
public class RecommendationApprovalService {

	private final RecommendationRecordStore recommendationRecordStore;
	private final RecommendationApprovalStore approvalStore;
	private final RecommendationApprovalIdGenerator idGenerator;
	private final RecommendationApprovalAuditLogger auditLogger;
	private final RecommendationApprovalAuditMapper auditMapper;
	private final ApprovalMetricsRecorder metricsRecorder;

	public RecommendationApprovalService(
			RecommendationRecordStore recommendationRecordStore,
			RecommendationApprovalStore approvalStore,
			RecommendationApprovalIdGenerator idGenerator,
			RecommendationApprovalAuditLogger auditLogger,
			RecommendationApprovalAuditMapper auditMapper,
			ApprovalMetricsRecorder metricsRecorder
	) {
		this.recommendationRecordStore = recommendationRecordStore;
		this.approvalStore = approvalStore;
		this.idGenerator = idGenerator;
		this.auditLogger = auditLogger;
		this.auditMapper = auditMapper;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<RecommendationApprovalResponse> decide(
			String recommendationRecordId,
			RecommendationApprovalRequest request
	) {
		return validateRequest(request)
				.then(recommendationRecordStore.findById(recommendationRecordId))
				.switchIfEmpty(Mono.error(new RecommendationApprovalRejectedException(
						"RECOMMENDATION_RECORD_NOT_FOUND",
						"Recommendation record not found."
				)))
				.flatMap(record -> saveDecision(record, request));
	}

	public Mono<RecommendationApprovalRecord> findLatest(String recommendationRecordId) {
		return approvalStore.findLatestByRecommendationRecordId(recommendationRecordId);
	}

	private Mono<Void> validateRequest(RecommendationApprovalRequest request) {
		if (request == null) {
			return Mono.error(new RecommendationApprovalRejectedException(
					"APPROVAL_REQUEST_REQUIRED",
					"Approval request is required."
			));
		}

		if (request.decision() == null) {
			return Mono.error(new RecommendationApprovalRejectedException(
					"APPROVAL_DECISION_REQUIRED",
					"Approval decision is required."
			));
		}

		if (request.operatorId() == null || request.operatorId().isBlank()) {
			return Mono.error(new RecommendationApprovalRejectedException(
					"OPERATOR_ID_REQUIRED",
					"operatorId is required."
			));
		}

		if (request.reason() == null || request.reason().isBlank()) {
			return Mono.error(new RecommendationApprovalRejectedException(
					"APPROVAL_REASON_REQUIRED",
					"reason is required."
			));
		}

		return Mono.empty();
	}

	private Mono<RecommendationApprovalResponse> saveDecision(
			RecommendationRecord recommendation,
			RecommendationApprovalRequest request
	) {
		RecommendationApprovalRecord record = new RecommendationApprovalRecord(
				idGenerator.generate(),
				recommendation.recommendationRecordId(),
				recommendation.incidentId(),
				request.decision().toStatus(),
				request.operatorId(),
				request.reason(),
				Instant.now(),
				safeMetadata(request.metadata())
		);

		return approvalStore.save(record)
				.doOnNext(metricsRecorder::recordDecision)
				.flatMap(saved ->
						auditLogger.log(auditMapper.toAuditLog(saved))
								.onErrorResume(ex -> Mono.empty())
								.thenReturn(saved)
				)
				.map(this::toResponse);
	}

	private RecommendationApprovalResponse toResponse(RecommendationApprovalRecord record) {
		return new RecommendationApprovalResponse(
				record.approvalId(),
				record.recommendationRecordId(),
				record.incidentId(),
				record.status(),
				record.operatorId(),
				record.reason()
		);
	}

	private Map<String, String> safeMetadata(Map<String, String> metadata) {
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
