package com.fintech.sre.agent.postmortem;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class RootCauseHypothesisGenerator {

	public Mono<List<String>> generate(PostmortemGenerationInput input) {
		List<String> hypotheses = new ArrayList<>();
		var metricsSnapshot = input.context().incidentContext().metricsSnapshot();

		if (metricsSnapshot != null
				&& metricsSnapshot.redisTimeoutCount() != null
				&& metricsSnapshot.redisTimeoutCount() > 0) {
			hypotheses.add("Redis timeout 증가가 payment-api latency 증가에 영향을 주었을 가능성");
		}

		if (metricsSnapshot != null
				&& metricsSnapshot.dbConnectionPending() != null
				&& metricsSnapshot.dbConnectionPending() > 0) {
			hypotheses.add("Retry 증가 또는 fallback 부하로 DB connection pending이 발생했을 가능성");
		}

		if (hypotheses.isEmpty()) {
			hypotheses.add("현재 데이터만으로 Root Cause 후보를 명확히 판단하기 어려움");
		}

		return Mono.just(hypotheses);
	}
}
