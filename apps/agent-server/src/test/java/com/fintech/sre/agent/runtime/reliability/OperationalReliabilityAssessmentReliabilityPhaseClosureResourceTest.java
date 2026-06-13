package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityAssessmentReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityAssessmentReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-assessment-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Assessment Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Assessment Reliability Semantics");
		assertThat(markdown).contains("## 4. Evidence Reliability Dependency");
		assertThat(markdown).contains("## 5. Assessment Reliability Integration Semantics");
		assertThat(markdown).contains("## 6. Payment Safety / Contradiction Propagation");
		assertThat(markdown).contains("## 7. Operator-Facing Assessment Boundary");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("AssessmentReliability");
		assertThat(markdown).contains("AssessmentReliabilityEvaluator");
		assertThat(markdown).contains("AssessmentReliabilityLevel");
		assertThat(markdown).contains("AssessmentReliabilityReason");
		assertThat(markdown).contains("AssessmentReliabilityScope");
		assertThat(markdown).contains("AssessmentReliabilityIntegration");
		assertThat(markdown).contains("AssessmentReliabilityIntegrationResult");
		assertThat(markdown).contains("AssessmentReliabilityIntegrationStatus");
		assertThat(markdown).contains("AssessmentReliabilityIntegrationReason");
		assertThat(markdown).contains("AssessmentReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"AssessmentReliability는 EvidenceReliability 위의 assessment 단계 신뢰도"
		);
		assertThat(markdown).contains("AssessmentReliability는 read-only");
		assertThat(markdown).contains("AssessmentReliability는 assessment mutation이 아님");
		assertThat(markdown).contains("AssessmentReliability는 recommendation이 아님");
		assertThat(markdown).contains("AssessmentReliability는 execution permission이 아님");
		assertThat(markdown).contains("AssessmentReliability는 ActionCommand admission이 아님");
		assertThat(markdown).contains("BLOCKED evidence reliability → assessment BLOCKED");
		assertThat(markdown).contains("UNRELIABLE evidence reliability → assessment UNRELIABLE");
		assertThat(markdown).contains("LOW evidence reliability → assessment downgrade");
		assertThat(markdown).contains("insufficient confidence → assessment certainty 금지");
		assertThat(markdown).contains("payment safety uncertainty → assessment downgrade");
		assertThat(markdown).contains("payment safety uncertainty → lifecycle risk 전파");
		assertThat(markdown).contains("contradictory evidence/assessment → lifecycle uncertainty 전파");
		assertThat(markdown).contains(
				"HIGH assessment reliability는 HIGH evidence reliability + no payment uncertainty + no contradiction 필요"
		);
		assertThat(markdown).contains("BLOCKED assessment reliability는 lifecycle stable 금지");
		assertThat(markdown).contains("UNRELIABLE assessment reliability는 recommendation certainty 금지");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persisted assessment reliability history");
		assertThat(markdown).contains("assessment reliability trend analysis");
		assertThat(markdown).contains("policy-configurable assessment reliability rules");
		assertThat(markdown).contains("SRE Console assessment reliability visualization");
		assertThat(markdown).contains("Decision Reliability integration");
		assertThat(markdown).contains("Recommendation Reliability integration");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Assessment Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-assessment-reliability-phase-closure.md"
		);
	}
}
