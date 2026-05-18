package com.fintech.sre.agent.reanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ReanalysisCandidateServiceTest {

	@Test
	void shouldCreateCandidate() {
		InMemoryReanalysisCandidateStore store =
				new InMemoryReanalysisCandidateStore();
		ReanalysisCandidateService service = new ReanalysisCandidateService(
				store,
				new ReanalysisCandidateIdGenerator()
		);

		ReanalysisCandidateResponse response = service.create(
				"incident-1",
				new ReanalysisCandidateRequest(
						"verification-1",
						"execution-1",
						ReanalysisTriggerReason.NOT_VERIFIED,
						"operator-a",
						"Verification failed and needs re-analysis.",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.status())
				.isEqualTo(
						ReanalysisCandidateStatus.PENDING_REANALYSIS
				);

		ReanalysisTriggerCandidate candidate =
				store.findByIncidentId("incident-1").blockFirst();

		assertThat(candidate.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload");
	}

	@Test
	void shouldRequireReasonOperatorAndSummary() {
		ReanalysisCandidateService service = new ReanalysisCandidateService(
				new InMemoryReanalysisCandidateStore(),
				new ReanalysisCandidateIdGenerator()
		);

		assertThatThrownBy(() -> service.create(
				"incident-1",
				new ReanalysisCandidateRequest(
						null,
						null,
						null,
						"",
						"",
						Map.of()
				)
		).block())
				.isInstanceOf(ReanalysisCandidateRejectedException.class)
				.hasMessage("reason is required.");
	}
}
