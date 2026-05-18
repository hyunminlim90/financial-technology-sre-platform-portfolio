package com.fintech.sre.agent.recommendation.persistence;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.observability.metrics.RecommendationMetricsRecorder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RecommendationPersistenceService {

	private final RecommendationRecordMapper mapper;
	private final RecommendationRecordStore store;
	private final RecommendationMetricsRecorder metricsRecorder;

	public RecommendationPersistenceService(
			RecommendationRecordMapper mapper,
			RecommendationRecordStore store,
			RecommendationMetricsRecorder metricsRecorder
	) {
		this.mapper = mapper;
		this.store = store;
		this.metricsRecorder = metricsRecorder;
	}

	public Flux<RecommendationRecord> persist(
			String auditId,
			List<AlertEvent> alerts,
			List<IncidentRecommendationResponse> responses
	) {
		if (responses == null || responses.isEmpty()) {
			return Flux.empty();
		}

		List<AlertEvent> safeAlerts = alerts == null ? List.of() : alerts;

		return Flux.range(0, responses.size())
				.flatMap(index -> {
					IncidentRecommendationResponse response = responses.get(index);
					AlertEvent alert = index < safeAlerts.size() ? safeAlerts.get(index) : null;
					RecommendationRecord record = mapper.toRecord(auditId, alert, response);

					return store.save(record)
							.doOnNext(metricsRecorder::recordCreated)
							.onErrorResume(ex -> Mono.empty());
				});
	}

	public Mono<RecommendationRecord> findById(String id) {
		return store.findById(id);
	}

	public Flux<RecommendationRecord> findByIncidentId(String incidentId) {
		return store.findByIncidentId(incidentId);
	}

	public Flux<RecommendationRecord> findRecent(int limit) {
		return store.findRecent(limit);
	}
}
