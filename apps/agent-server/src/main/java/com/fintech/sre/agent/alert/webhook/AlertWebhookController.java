package com.fintech.sre.agent.alert.webhook;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertToIncidentRequestMapper;
import com.fintech.sre.agent.alert.audit.AlertIngestionAuditIdGenerator;
import com.fintech.sre.agent.alert.audit.AlertIngestionAuditLog;
import com.fintech.sre.agent.alert.audit.AlertIngestionAuditLogger;
import com.fintech.sre.agent.alert.dedup.AlertDeduplicationResult;
import com.fintech.sre.agent.alert.dedup.AlertDeduplicationService;
import com.fintech.sre.agent.alert.ratelimit.AlertRateLimitResult;
import com.fintech.sre.agent.alert.ratelimit.AlertRateLimitService;
import com.fintech.sre.agent.alert.summary.AlertBatchSummary;
import com.fintech.sre.agent.alert.summary.AlertBatchSummaryBuilder;
import com.fintech.sre.agent.decision.DecisionEngine;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.recommendation.persistence.RecommendationPersistenceService;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class AlertWebhookController {

	private final PrometheusAlertEventNormalizer normalizer;
	private final AlertToIncidentRequestMapper mapper;
	private final DecisionEngine decisionEngine;
	private final AlertIngestionAuditLogger auditLogger;
	private final AlertIngestionAuditIdGenerator auditIdGenerator;
	private final AlertDeduplicationService deduplicationService;
	private final AlertRateLimitService rateLimitService;
	private final AlertBatchSummaryBuilder batchSummaryBuilder;
	private final RecommendationPersistenceService recommendationPersistenceService;

	public AlertWebhookController(
			PrometheusAlertEventNormalizer normalizer,
			AlertToIncidentRequestMapper mapper,
			DecisionEngine decisionEngine,
			AlertIngestionAuditLogger auditLogger,
			AlertIngestionAuditIdGenerator auditIdGenerator,
			AlertDeduplicationService deduplicationService,
			AlertRateLimitService rateLimitService,
			AlertBatchSummaryBuilder batchSummaryBuilder,
			RecommendationPersistenceService recommendationPersistenceService
	) {
		this.normalizer = normalizer;
		this.mapper = mapper;
		this.decisionEngine = decisionEngine;
		this.auditLogger = auditLogger;
		this.auditIdGenerator = auditIdGenerator;
		this.deduplicationService = deduplicationService;
		this.rateLimitService = rateLimitService;
		this.batchSummaryBuilder = batchSummaryBuilder;
		this.recommendationPersistenceService = recommendationPersistenceService;
	}

	@PostMapping("/internal/alerts/prometheus")
	public Mono<ResponseEntity<AlertRecommendationResponse>> receivePrometheusAlert(
			@RequestBody PrometheusAlertWebhookRequest request
	) {
		String auditId = auditIdGenerator.generate();
		List<AlertEvent> alerts = normalizer.normalize(request);

		return Flux.fromIterable(alerts)
				.flatMap(alert ->
						deduplicationService.checkAndRecord(alert)
								.flatMap(dedup -> {
									if (dedup.duplicate()) {
										return Mono.just(new AlertProcessingItem(alert, dedup, null));
									}

									return rateLimitService.checkAndConsume(alert)
											.map(rateLimit -> new AlertProcessingItem(alert, dedup, rateLimit));
								})
				)
				.collectList()
				.flatMap(items -> {
					List<AlertEvent> recommendationCandidates = items.stream()
							.filter(item -> !item.deduplicationResult().duplicate())
							.filter(item -> item.rateLimitResult() == null || item.rateLimitResult().allowed())
							.map(AlertProcessingItem::alert)
							.toList();

					List<String> suppressedAlertIds = items.stream()
							.filter(item -> item.deduplicationResult().duplicate())
							.map(item -> item.alert().alertId())
							.toList();

					List<String> rateLimitedAlertIds = items.stream()
							.filter(item -> !item.deduplicationResult().duplicate())
							.filter(item -> item.rateLimitResult() != null && !item.rateLimitResult().allowed())
							.map(item -> item.alert().alertId())
							.toList();

					return Flux.fromIterable(recommendationCandidates)
							.map(mapper::toRequest)
							.flatMap(decisionEngine::decide)
							.collectList()
							.flatMap(recommendations ->
									recommendationPersistenceService.persist(auditId, recommendationCandidates, recommendations)
											.map(RecommendationRecord::recommendationRecordId)
											.collectList()
											.map(recordIds -> {
								AlertBatchSummary summary = batchSummaryBuilder.build(
										alerts,
										recommendations.size(),
										suppressedAlertIds.size(),
										rateLimitedAlertIds.size()
								);

								return new AlertRecommendationResponse(
										auditId,
										status(suppressedAlertIds, rateLimitedAlertIds),
										alerts.size(),
										recommendations.size(),
										suppressedAlertIds.size(),
										suppressedAlertIds,
										rateLimitedAlertIds.size(),
										rateLimitedAlertIds,
										summary,
										recordIds,
										recommendations
								);
											})
							);
				})
				.flatMap(response -> audit(auditId, alerts, response)
						.onErrorResume(ex -> Mono.empty())
						.thenReturn(ResponseEntity.ok(response)));
	}

	private Mono<Void> audit(
			String auditId,
			List<AlertEvent> alerts,
			AlertRecommendationResponse response
	) {
		if (alerts == null || alerts.isEmpty()) {
			return auditLogger.log(new AlertIngestionAuditLog(
					auditId,
					java.time.Instant.now(),
					"PROMETHEUS_ALERTMANAGER",
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					0,
					0,
					List.of(),
					0,
					List.of(),
					batchSummaryBuilder.build(List.of(), 0, 0, 0),
					List.of(),
					List.of("No alerts were normalized from webhook payload.")
			));
		}

		return Flux.fromIterable(alerts)
				.flatMap(alert -> auditLogger.log(new AlertIngestionAuditLog(
						auditId,
						java.time.Instant.now(),
						alert.source().name(),
						alert.alertId(),
						alert.alertName(),
						alert.status(),
						alert.severity().name(),
						alert.service(),
						alert.domain(),
						alert.namespace(),
						response.generatedRecommendations(),
						response.suppressedDuplicates(),
						response.suppressedAlertIds(),
						response.rateLimitedAlerts(),
						response.rateLimitedAlertIds(),
						response.batchSummary(),
						recommendationIds(response),
						List.of()
				)))
				.then();
	}

	private String status(
			List<String> suppressedAlertIds,
			List<String> rateLimitedAlertIds
	) {
		if (!rateLimitedAlertIds.isEmpty()) {
			return "RECOMMENDATION_GENERATED_WITH_RATE_LIMITING";
		}

		if (!suppressedAlertIds.isEmpty()) {
			return "RECOMMENDATION_GENERATED_WITH_DUPLICATE_SUPPRESSION";
		}

		return "RECOMMENDATION_GENERATED";
	}

	private List<String> recommendationIds(AlertRecommendationResponse response) {
		if (response == null || response.recommendations() == null) {
			return List.of();
		}

		return response.recommendations().stream()
				.map(IncidentRecommendationResponse::incidentId)
				.filter(id -> id != null && !id.isBlank())
				.toList();
	}

	private record AlertProcessingItem(
			AlertEvent alert,
			AlertDeduplicationResult deduplicationResult,
			AlertRateLimitResult rateLimitResult
	) {
	}
}
