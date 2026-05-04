package com.fintech.sre.agent.evidence;

import reactor.core.publisher.Mono;

public interface EvidenceContextProvider {

	Mono<EvidenceContext> provide(String incidentId);
}
