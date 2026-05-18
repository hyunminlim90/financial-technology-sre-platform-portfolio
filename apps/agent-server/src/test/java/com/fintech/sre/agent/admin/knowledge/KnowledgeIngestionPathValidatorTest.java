package com.fintech.sre.agent.admin.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class KnowledgeIngestionPathValidatorTest {

	@Test
	void shouldAllowPathUnderAllowedRoot() {
		KnowledgeAdminProperties properties = new KnowledgeAdminProperties(
				true,
				List.of("/tmp/portfolio")
		);

		KnowledgeIngestionPathValidator validator = new KnowledgeIngestionPathValidator(properties);

		assertThat(validator.validate("/tmp/portfolio/scenarios"))
				.isNotNull();
	}

	@Test
	void shouldRejectPathOutsideAllowedRoot() {
		KnowledgeAdminProperties properties = new KnowledgeAdminProperties(
				true,
				List.of("/tmp/portfolio")
		);

		KnowledgeIngestionPathValidator validator = new KnowledgeIngestionPathValidator(properties);

		assertThatThrownBy(() -> validator.validate("/etc"))
				.isInstanceOf(KnowledgeIngestionRejectedException.class);
	}

	@Test
	void shouldRejectWhenDisabled() {
		KnowledgeAdminProperties properties = new KnowledgeAdminProperties(
				false,
				List.of("/tmp/portfolio")
		);

		KnowledgeIngestionPathValidator validator = new KnowledgeIngestionPathValidator(properties);

		assertThatThrownBy(() -> validator.validate("/tmp/portfolio"))
				.isInstanceOf(KnowledgeIngestionRejectedException.class);
	}
}
