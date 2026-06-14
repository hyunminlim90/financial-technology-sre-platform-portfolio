package com.fintech.sre.agent.runtime.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationContentPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalRecommendationContentPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-recommendation-content-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Recommendation Content Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Recommendation Content Semantics");
		assertThat(markdown).contains("## 4. Recommendation Candidate Dependency");
		assertThat(markdown).contains("## 5. Required Content Model");
		assertThat(markdown).contains(
				"## 6. Recommendation Content Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Operator-Facing Exposure Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Content Protection Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("RecommendationContent");
		assertThat(markdown).contains("RecommendationContentBuilder");
		assertThat(markdown).contains("RecommendationContentType");
		assertThat(markdown).contains("RecommendationContentReason");
		assertThat(markdown).contains("RecommendationContentScope");
		assertThat(markdown).contains("RecommendationContentIntegration");
		assertThat(markdown).contains("RecommendationContentIntegrationResult");
		assertThat(markdown).contains("RecommendationContentIntegrationStatus");
		assertThat(markdown).contains("RecommendationContentIntegrationReason");
		assertThat(markdown).contains("RecommendationContentIntegrationScope");

		assertThat(markdown).contains("RecommendationContent는 operator-facing recommendation data model이다.");
		assertThat(markdown).contains("RecommendationContent는 read-only이다.");
		assertThat(markdown).contains("RecommendationContent는 actual recommendation이 아니다.");
		assertThat(markdown).contains("RecommendationContent는 recommendation execution이 아니다.");
		assertThat(markdown).contains("RecommendationContent는 LLM output이 아니다.");
		assertThat(markdown).contains("RecommendationContent는 RAG retrieval result가 아니다.");
		assertThat(markdown).contains("RecommendationContent는 runbook selection이 아니다.");
		assertThat(markdown).contains("RecommendationContent는 approval request가 아니다.");
		assertThat(markdown).contains("RecommendationContent는 ActionCommand가 아니다.");
		assertThat(markdown).contains("RecommendationContent는 execution permission이 아니다.");
		assertThat(markdown).contains("RecommendationContent는 ELIGIBLE RecommendationCandidate에 의존한다.");
		assertThat(markdown).contains("scenarioId는 필수이다.");
		assertThat(markdown).contains("runbookId는 필수이다.");
		assertThat(markdown).contains("rollbackId는 필수이다.");
		assertThat(markdown).contains("verificationId는 필수이다.");
		assertThat(markdown).contains("paymentSafetyClassification은 필수이다.");
		assertThat(markdown).contains("RecommendationContentIntegration은 operator-facing content exposure readiness 계층이다.");
		assertThat(markdown).contains("payment safety classification missing은 BLOCKED이다.");
		assertThat(markdown).contains("payment safety classification invalid는 BLOCKED이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("raw payload 노출은 금지된다.");
		assertThat(markdown).contains("vendor detail 노출은 금지된다.");
		assertThat(markdown).contains("credential 노출은 금지된다.");
		assertThat(markdown).contains("configuration secret 노출은 금지된다.");
		assertThat(markdown).contains("RecommendationContentIntegration은 approval authority가 아니다.");
		assertThat(markdown).contains("RecommendationContentIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("RecommendationContentIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Recommendation Content");
		assertThat(markdown).contains("Recommendation Engine");
		assertThat(markdown).contains("LLM");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Runbook Selector");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("Execution Authority");

		assertThat(markdown).contains("Actual Recommendation Generation");
		assertThat(markdown).contains("Recommendation Rendering");
		assertThat(markdown).contains("LLM Prompt Integration");
		assertThat(markdown).contains("LLM Response Integration");
		assertThat(markdown).contains("RAG Retrieval Integration");
		assertThat(markdown).contains("Runbook Selection");
		assertThat(markdown).contains("Approval Request Generation");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Recommendation Persistence");
		assertThat(markdown).contains("Recommendation API Exposure");
		assertThat(markdown).contains("Recommendation Audit History");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Recommendation Content Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-recommendation-content-phase-closure.md"
		);
	}
}
