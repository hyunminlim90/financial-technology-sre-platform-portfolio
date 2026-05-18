package com.fintech.sre.agent.recommendation.verification;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.VerificationMetricsRecorder;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;

import reactor.core.publisher.Mono;

@Service("recommendationVerificationResultService")
public class VerificationResultService {

	private final HumanExecutionResultStore executionResultStore;
	private final VerificationResultStore verificationResultStore;
	private final VerificationResultIdGenerator idGenerator;
	private final VerificationMetricsRecorder metricsRecorder;

	public VerificationResultService(
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationResultStore,
			VerificationResultIdGenerator idGenerator,
			VerificationMetricsRecorder metricsRecorder
	) {
		this.executionResultStore = executionResultStore;
		this.verificationResultStore = verificationResultStore;
		this.idGenerator = idGenerator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<VerificationResultResponse> verify(
			String executionResultId,
			VerificationResultRequest request
	) {
		return validate(request)
				.then(executionResultStore.findById(executionResultId))
				.switchIfEmpty(Mono.error(
						new VerificationResultRejectedException(
								"EXECUTION_RESULT_NOT_FOUND",
								"Execution result not found."
						)
				))
				.flatMap(result -> save(result, request));
	}

	public Mono<VerificationResultRecord> findById(
			String verificationResultId
	) {
		return verificationResultStore.findById(verificationResultId);
	}

	private Mono<Void> validate(
			VerificationResultRequest request
	) {
		if (request == null) {
			return Mono.error(
					new VerificationResultRejectedException(
							"VERIFICATION_REQUEST_REQUIRED",
							"Verification request is required."
					)
			);
		}

		if (request.status() == null) {
			return Mono.error(
					new VerificationResultRejectedException(
							"VERIFICATION_STATUS_REQUIRED",
							"status is required."
					)
			);
		}

		if (request.operatorId() == null
				|| request.operatorId().isBlank()) {
			return Mono.error(
					new VerificationResultRejectedException(
							"OPERATOR_ID_REQUIRED",
							"operatorId is required."
					)
			);
		}

		if (request.summary() == null
				|| request.summary().isBlank()) {
			return Mono.error(
					new VerificationResultRejectedException(
							"VERIFICATION_SUMMARY_REQUIRED",
							"summary is required."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<VerificationResultResponse> save(
			HumanExecutionResultRecord executionResult,
			VerificationResultRequest request
	) {
		VerificationResultRecord record =
				new VerificationResultRecord(
						idGenerator.generate(),
						executionResult.executionResultId(),
						executionResult.executionPlanId(),
						executionResult.recommendationRecordId(),
						executionResult.incidentId(),
						request.status(),
						request.operatorId(),
						request.summary(),
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return verificationResultStore.save(record)
				.doOnNext(metricsRecorder::recordVerification)
				.map(this::toResponse);
	}

	private VerificationResultResponse toResponse(
			VerificationResultRecord record
	) {
		return new VerificationResultResponse(
				record.verificationResultId(),
				record.executionResultId(),
				record.executionPlanId(),
				record.recommendationRecordId(),
				record.incidentId(),
				record.status(),
				record.operatorId(),
				record.summary()
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
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("password")
				&& !lower.contains("payment")
				&& !lower.contains("rawlog");
	}
}
