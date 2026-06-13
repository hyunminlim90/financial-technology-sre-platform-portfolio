package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityRecommendationReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-recommendation-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Recommendation Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Recommendation Reliability Semantics");
		assertThat(markdown).contains("## 4. Decision Reliability Dependency");
		assertThat(markdown).contains(
				"## 5. Human Approval / Rollback / Verification Requirements"
		);
		assertThat(markdown).contains(
				"## 6. Recommendation Reliability Integration Semantics"
		);
		assertThat(markdown).contains(
				"## 7. Payment Safety / Contradiction Propagation"
		);
		assertThat(markdown).contains(
				"## 8. Operator-Facing Recommendation Boundary"
		);
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("RecommendationReliability");
		assertThat(markdown).contains("RecommendationReliabilityEvaluator");
		assertThat(markdown).contains("RecommendationReliabilityLevel");
		assertThat(markdown).contains("RecommendationReliabilityReason");
		assertThat(markdown).contains("RecommendationReliabilityScope");
		assertThat(markdown).contains("RecommendationReliabilityIntegration");
		assertThat(markdown).contains("RecommendationReliabilityIntegrationResult");
		assertThat(markdown).contains("RecommendationReliabilityIntegrationStatus");
		assertThat(markdown).contains("RecommendationReliabilityIntegrationReason");
		assertThat(markdown).contains("RecommendationReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"RecommendationReliability는 DecisionReliability 위의 operator-facing recommendation 신뢰도"
		);
		assertThat(markdown).contains("RecommendationReliability는 read-only");
		assertThat(markdown).contains("RecommendationReliability는 recommendation mutation이 아님");
		assertThat(markdown).contains("RecommendationReliability는 실제 recommendation 생성이 아님");
		assertThat(markdown).contains("RecommendationReliability는 execution permission이 아님");
		assertThat(markdown).contains("RecommendationReliability는 ActionCommand admission이 아님");
		assertThat(markdown).contains("RecommendationReliability는 human approval이 아님");
		assertThat(markdown).contains("BLOCKED decision reliability → recommendation BLOCKED");
		assertThat(markdown).contains("UNRELIABLE decision reliability → recommendation UNRELIABLE");
		assertThat(markdown).contains("LOW decision reliability → recommendation downgrade");
		assertThat(markdown).contains("missing human approval requirement → recommendation BLOCKED");
		assertThat(markdown).contains("missing rollback binding → recommendation BLOCKED");
		assertThat(markdown).contains("missing verification binding → recommendation BLOCKED");
		assertThat(markdown).contains("payment safety uncertainty → recommendation downgrade");
		assertThat(markdown).contains("payment safety uncertainty → lifecycle CRITICAL risk 유지");
		assertThat(markdown).contains("contradictory decision/recommendation → lifecycle uncertainty 전파");
		assertThat(markdown).contains("BLOCKED recommendation reliability는 operator-facing recommendation 금지");
		assertThat(markdown).contains("UNRELIABLE recommendation reliability는 recommendation certainty 금지");
		assertThat(markdown).contains(
				"HIGH recommendation reliability는 HIGH decision reliability + human approval required + rollback binding + verification binding + no payment uncertainty + no contradiction 필요"
		);
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persisted recommendation reliability history");
		assertThat(markdown).contains("recommendation reliability trend analysis");
		assertThat(markdown).contains("policy-configurable recommendation reliability rules");
		assertThat(markdown).contains("SRE Console recommendation reliability visualization");
		assertThat(markdown).contains("Human Approval integration");
		assertThat(markdown).contains("Approval Reliability");
		assertThat(markdown).contains("Verification Reliability");
		assertThat(markdown).contains("Action Admission integration");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Recommendation Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-recommendation-reliability-phase-closure.md"
		);
	}
}
