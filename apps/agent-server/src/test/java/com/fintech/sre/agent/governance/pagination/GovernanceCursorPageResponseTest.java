package com.fintech.sre.agent.governance.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class GovernanceCursorPageResponseTest {

	@Test
	void shouldRetainItemsAndMetadata() {
		GovernanceCursorMetadata metadata = new GovernanceCursorMetadata(
				"next-cursor",
				"previous-cursor",
				true,
				false,
				50,
				GovernanceCursorDirection.NEXT,
				"occurredAt DESC, recordId DESC"
		);

		GovernanceCursorPageResponse<String> response =
				new GovernanceCursorPageResponse<>(
						List.of("item-1", "item-2"),
						metadata
				);

		assertThat(response.items()).containsExactly("item-1", "item-2");
		assertThat(response.page().nextCursor()).isEqualTo("next-cursor");
		assertThat(response.page().previousCursor()).isEqualTo("previous-cursor");
		assertThat(response.page().hasNext()).isTrue();
		assertThat(response.page().hasPrevious()).isFalse();
		assertThat(response.page().limit()).isEqualTo(50);
		assertThat(response.page().direction()).isEqualTo(
				GovernanceCursorDirection.NEXT
		);
		assertThat(response.page().ordering()).isEqualTo(
				"occurredAt DESC, recordId DESC"
		);
	}
}
