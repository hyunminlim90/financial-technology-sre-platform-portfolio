package com.fintech.sre.agent.recommendation.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;

class VerificationResultServiceTest {

	@Test
	void shouldSaveVerificationWhenExecutionResultExists() {
		InMemoryHumanExecutionResultStore executionResultStore =
				new InMemoryHumanExecutionResultStore();
		InMemoryVerificationResultStore verificationResultStore =
				new InMemoryVerificationResultStore();

		executionResultStore.save(new HumanExecutionResultRecord(
				"execution-result-1",
				"execution-plan-1",
				"recommendation-1",
				"incident-1",
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"manual action applied",
				Instant.now().minusSeconds(60),
				Instant.now().minusSeconds(30),
				Instant.now(),
				Map.of()
		)).block();

		VerificationResultService service = new VerificationResultService(
				executionResultStore,
				verificationResultStore,
				new VerificationResultIdGenerator(),
				MetricsRecorderTestSupport.verificationMetricsRecorder()
		);

		VerificationResultResponse response = service.verify(
				"execution-result-1",
				new VerificationResultRequest(
						VerificationStatus.VERIFIED,
						"operator-a",
						"Latency normalized",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.status())
				.isEqualTo(VerificationStatus.VERIFIED);

		VerificationResultRecord record =
				verificationResultStore.findById(response.verificationResultId()).block();

		assertThat(record.metadata())
				.containsKey("team")
				.doesNotContainKey("paymentPayload");
	}

	@Test
	void shouldRejectWhenExecutionResultMissing() {
		VerificationResultService service = new VerificationResultService(
				new InMemoryHumanExecutionResultStore(),
				new InMemoryVerificationResultStore(),
				new VerificationResultIdGenerator(),
				MetricsRecorderTestSupport.verificationMetricsRecorder()
		);

		assertThatThrownBy(() -> service.verify(
				"missing",
				new VerificationResultRequest(
						VerificationStatus.NOT_VERIFIED,
						"operator-a",
						"Could not verify",
						Map.of()
				)
		).block())
				.isInstanceOf(VerificationResultRejectedException.class)
				.hasMessage("Execution result not found.");
	}
}
