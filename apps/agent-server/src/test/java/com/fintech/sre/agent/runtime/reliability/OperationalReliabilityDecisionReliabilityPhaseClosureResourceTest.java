package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityDecisionReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityDecisionReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-decision-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Decision Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Decision Reliability Semantics");
		assertThat(markdown).contains("## 4. Assessment Reliability Dependency");
		assertThat(markdown).contains("## 5. Scenario / Rollback / Verification Binding Requirements");
		assertThat(markdown).contains("## 6. Decision Reliability Integration Semantics");
		assertThat(markdown).contains("## 7. Payment Safety / Contradiction Propagation");
		assertThat(markdown).contains("## 8. Operator-Facing Decision Boundary");
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("DecisionReliability");
		assertThat(markdown).contains("DecisionReliabilityEvaluator");
		assertThat(markdown).contains("DecisionReliabilityLevel");
		assertThat(markdown).contains("DecisionReliabilityReason");
		assertThat(markdown).contains("DecisionReliabilityScope");
		assertThat(markdown).contains("DecisionReliabilityIntegration");
		assertThat(markdown).contains("DecisionReliabilityIntegrationResult");
		assertThat(markdown).contains("DecisionReliabilityIntegrationStatus");
		assertThat(markdown).contains("DecisionReliabilityIntegrationReason");
		assertThat(markdown).contains("DecisionReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"DecisionReliability는 AssessmentReliability 위의 decision-stage 신뢰도"
		);
		assertThat(markdown).contains("DecisionReliability는 read-only");
		assertThat(markdown).contains("DecisionReliability는 decision mutation이 아님");
		assertThat(markdown).contains("DecisionReliability는 recommendation이 아님");
		assertThat(markdown).contains("DecisionReliability는 execution permission이 아님");
		assertThat(markdown).contains("DecisionReliability는 ActionCommand admission이 아님");
		assertThat(markdown).contains("DecisionReliability는 실제 action decision이 아님");
		assertThat(markdown).contains("BLOCKED assessment reliability → decision BLOCKED");
		assertThat(markdown).contains("UNRELIABLE assessment reliability → decision UNRELIABLE");
		assertThat(markdown).contains("LOW assessment reliability → decision downgrade");
		assertThat(markdown).contains("missing scenario binding → decision BLOCKED");
		assertThat(markdown).contains("missing rollback binding → decision BLOCKED");
		assertThat(markdown).contains("missing verification binding → decision BLOCKED");
		assertThat(markdown).contains("payment safety uncertainty → decision downgrade");
		assertThat(markdown).contains("payment safety uncertainty → lifecycle risk 전파");
		assertThat(markdown).contains("contradictory assessment/decision → lifecycle uncertainty 전파");
		assertThat(markdown).contains(
				"HIGH decision reliability는 HIGH assessment reliability + scenario binding + rollback binding + verification binding + no payment uncertainty + no contradiction 필요"
		);
		assertThat(markdown).contains("BLOCKED decision reliability는 lifecycle stable 금지");
		assertThat(markdown).contains("UNRELIABLE decision reliability는 recommendation certainty 금지");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persisted decision reliability history");
		assertThat(markdown).contains("decision reliability trend analysis");
		assertThat(markdown).contains("policy-configurable decision reliability rules");
		assertThat(markdown).contains("SRE Console decision reliability visualization");
		assertThat(markdown).contains("Recommendation Reliability integration");
		assertThat(markdown).contains("Human Approval integration");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Decision Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-decision-reliability-phase-closure.md"
		);
	}
}
