package com.fintech.sre.agent.runbook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionType;

class RunbookActionMapperTest {

	private final RunbookActionMapper mapper = new RunbookActionMapper();

	@Test
	void shouldMapRunbookActionToActionCommand() {
		RunbookAction action = new RunbookAction(
				"SCALE_OUT_WORKER",
				"QUEUE",
				"payment-worker",
				"HIGH",
				"SERVICE",
				"Scale out payment worker",
				List.of("KAFKA_CONSUMER_LAG_HIGH"),
				List.of("DB_POOL_PENDING_HIGH"),
				new RunbookApproval(true),
				new RunbookRollback(true, "Scale back replicas"),
				new RunbookVerification(true, List.of("Lag decreases")),
				new RunbookPaymentSafety(true, true, "MEDIUM")
		);

		ActionCommand command = mapper.toCommand(action);

		assertThat(command.type()).isEqualTo(ActionType.SCALE_OUT);
		assertThat(command.target().service()).isEqualTo("payment-worker");
		assertThat(command.target().environment()).isEqualTo("unknown");
		assertThat(command.verifications()).hasSize(2);
	}
}
