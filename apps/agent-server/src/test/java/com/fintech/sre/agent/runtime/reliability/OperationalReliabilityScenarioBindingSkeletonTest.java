package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationalReliabilityScenarioBindingSkeletonTest {

	private final ScenarioBinding binding = new ScenarioBinding();

	@Test
	void shouldReturnNoRecommendationWhenNoScenarioExists() {
		ScenarioBindingDecision decision = binding.bind(null);

		assertThat(decision.status()).isEqualTo(ScenarioBindingStatus.REJECTED);
		assertThat(decision.recommendationScenarioAvailable()).isFalse();
	}

	@Test
	void shouldReturnNoActionCommandWhenNoScenarioExists() {
		ScenarioBindingDecision decision = binding.bind(null);

		assertThat(decision.status()).isEqualTo(ScenarioBindingStatus.REJECTED);
		assertThat(decision.actionCommandScenarioAvailable()).isFalse();
	}

	@Test
	void shouldRejectWhenScenarioReferenceIsMissing() {
		ScenarioBindingDecision decision = binding.bind(null);

		assertThat(decision.status()).isEqualTo(ScenarioBindingStatus.REJECTED);
		assertThat(decision.rejectionReason())
				.isEqualTo(
						ScenarioBindingRejectionReason.MISSING_SCENARIO_REFERENCE
				);
	}

	@Test
	void shouldRejectUnknownScenario() {
		ScenarioBindingDecision decision = binding.bind(new ScenarioReference(
				"scenario-unknown",
				"portfolio-runtime",
				false,
				false
		));

		assertThat(decision.status()).isEqualTo(ScenarioBindingStatus.REJECTED);
		assertThat(decision.rejectionReason())
				.isEqualTo(ScenarioBindingRejectionReason.UNKNOWN_SCENARIO);
	}

	@Test
	void shouldApplyHighRiskRestrictionToDeprecatedScenario() {
		ScenarioBindingDecision decision = binding.bind(new ScenarioReference(
				"scenario-deprecated",
				"portfolio-runtime",
				true,
				true
		));

		assertThat(decision.status()).isEqualTo(ScenarioBindingStatus.RESTRICTED);
		assertThat(decision.highRiskRestricted()).isTrue();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						ScenarioBindingRejectionReason
								.DEPRECATED_SCENARIO_HIGH_RISK_RESTRICTION
				);
		assertThat(decision.recommendationScenarioAvailable()).isTrue();
		assertThat(decision.actionCommandScenarioAvailable()).isTrue();
	}

	@Test
	void shouldBindKnownNonDeprecatedScenario() {
		ScenarioBindingDecision decision = binding.bind(new ScenarioReference(
				"scenario-known",
				"portfolio-runtime",
				true,
				false
		));

		assertThat(decision.status()).isEqualTo(ScenarioBindingStatus.BOUND);
		assertThat(decision.rejectionReason()).isNull();
		assertThat(decision.recommendationScenarioAvailable()).isTrue();
		assertThat(decision.actionCommandScenarioAvailable()).isTrue();
	}

	@Test
	void shouldRemainSemanticPrerequisiteOnly() {
		ScenarioBindingDecision decision = binding.bind(new ScenarioReference(
				"scenario-known",
				"portfolio-runtime",
				true,
				false
		));

		assertThat(decision.semanticPrerequisiteOnly()).isTrue();
		assertThat(decision.executionPermission()).isFalse();
	}

	@Test
	void shouldReferencePortfolioKnowledgeWithoutMutatingIt() {
		ScenarioBindingDecision decision = binding.bind(new ScenarioReference(
				"scenario-known",
				"portfolio-runtime",
				true,
				false
		));

		assertThat(decision.scenarioReference().knowledgeSourceId())
				.isEqualTo("portfolio-runtime");
		assertThat(decision.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullScenarioIdAtReferenceConstruction() {
		assertThatThrownBy(() -> new ScenarioReference(
				null,
				"portfolio-runtime",
				true,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("scenarioId must not be null");
	}

	@Test
	void shouldRejectNullKnowledgeSourceIdAtReferenceConstruction() {
		assertThatThrownBy(() -> new ScenarioReference(
				"scenario-known",
				null,
				true,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("knowledgeSourceId must not be null");
	}
}
