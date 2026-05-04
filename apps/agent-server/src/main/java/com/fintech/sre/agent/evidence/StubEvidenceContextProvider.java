package com.fintech.sre.agent.evidence;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class StubEvidenceContextProvider implements EvidenceContextProvider {

	@Override
	public Mono<EvidenceContext> provide(String incidentId) {
		return Mono.just(new EvidenceContext(
				incidentId,
				"payment-service",
				"prod",
				List.of(
						new Evidence(
								EvidenceLayer.APPLICATION,
								EvidenceSignal.P99_LATENCY_HIGH,
								1,
								1,
								Duration.ofMinutes(5),
								EvidenceSource.PROMETHEUS,
								EvidenceSeverity.WARNING,
								EvidenceConfidence.HIGH,
								EvidenceStatus.PRESENT,
								"p95 latency spike"
						),
						new Evidence(
								EvidenceLayer.APPLICATION,
								EvidenceSignal.ERROR_RATE_HIGH,
								1,
								1,
								Duration.ofMinutes(5),
								EvidenceSource.PROMETHEUS,
								EvidenceSeverity.WARNING,
								EvidenceConfidence.HIGH,
								EvidenceStatus.PRESENT,
								"error rate spike"
						)
				),
				Map.of("region", "ap-northeast-2"),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		));
	}
}
