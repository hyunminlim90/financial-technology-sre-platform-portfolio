package com.fintech.sre.agent.reanalysis;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReanalysisCandidateService {

	private final ReanalysisCandidateStore store;
	private final ReanalysisCandidateIdGenerator idGenerator;

	public ReanalysisCandidateService(
			ReanalysisCandidateStore store,
			ReanalysisCandidateIdGenerator idGenerator
	) {
		this.store = store;
		this.idGenerator = idGenerator;
	}

	public Mono<ReanalysisCandidateResponse> create(
			String incidentId,
			ReanalysisCandidateRequest request
	) {
		return validate(request)
				.then(save(incidentId, request));
	}

	public Flux<ReanalysisTriggerCandidate> findByIncidentId(
			String incidentId
	) {
		return store.findByIncidentId(incidentId);
	}

	private Mono<Void> validate(
			ReanalysisCandidateRequest request
	) {
		if (request == null) {
			return Mono.error(
					new ReanalysisCandidateRejectedException(
							"REANALYSIS_REQUEST_REQUIRED",
							"Reanalysis request is required."
					)
			);
		}

		if (request.reason() == null) {
			return Mono.error(
					new ReanalysisCandidateRejectedException(
							"REANALYSIS_REASON_REQUIRED",
							"reason is required."
					)
			);
		}

		if (request.operatorId() == null
				|| request.operatorId().isBlank()) {
			return Mono.error(
					new ReanalysisCandidateRejectedException(
							"OPERATOR_ID_REQUIRED",
							"operatorId is required."
					)
			);
		}

		if (request.summary() == null
				|| request.summary().isBlank()) {
			return Mono.error(
					new ReanalysisCandidateRejectedException(
							"REANALYSIS_SUMMARY_REQUIRED",
							"summary is required."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<ReanalysisCandidateResponse> save(
			String incidentId,
			ReanalysisCandidateRequest request
	) {
		ReanalysisTriggerCandidate candidate =
				new ReanalysisTriggerCandidate(
						idGenerator.generate(),
						incidentId,
						request.sourceVerificationResultId(),
						request.sourceExecutionResultId(),
						request.reason(),
						ReanalysisCandidateStatus.PENDING_REANALYSIS,
						request.operatorId(),
						request.summary(),
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return store.save(candidate)
				.map(this::toResponse);
	}

	private ReanalysisCandidateResponse toResponse(
			ReanalysisTriggerCandidate candidate
	) {
		return new ReanalysisCandidateResponse(
				candidate.reanalysisCandidateId(),
				candidate.incidentId(),
				candidate.reason(),
				candidate.status(),
				candidate.operatorId(),
				candidate.summary()
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
