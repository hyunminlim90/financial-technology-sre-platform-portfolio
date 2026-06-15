package com.fintech.sre.agent.runtime.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationPresentationPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalRecommendationPresentationPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-recommendation-presentation-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Recommendation Presentation Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Recommendation Presentation Semantics");
		assertThat(markdown).contains(
				"## 4. Recommendation Model Integration Dependency"
		);
		assertThat(markdown).contains("## 5. Required Presentation Model");
		assertThat(markdown).contains(
				"## 6. Recommendation Presentation Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Operator Exposure Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Payload Protection Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("RecommendationPresentation");
		assertThat(markdown).contains("RecommendationPresentationBuilder");
		assertThat(markdown).contains("RecommendationPresentationStatus");
		assertThat(markdown).contains("RecommendationPresentationReason");
		assertThat(markdown).contains("RecommendationPresentationScope");
		assertThat(markdown).contains("RecommendationPresentationIntegration");
		assertThat(markdown).contains("RecommendationPresentationIntegrationResult");
		assertThat(markdown).contains("RecommendationPresentationIntegrationStatus");
		assertThat(markdown).contains("RecommendationPresentationIntegrationReason");
		assertThat(markdown).contains("RecommendationPresentationIntegrationScope");

		assertThat(markdown).contains("RecommendationPresentation은 operator-facing presentation model이다.");
		assertThat(markdown).contains("RecommendationPresentation은 read-only이다.");
		assertThat(markdown).contains("RecommendationPresentation은 UI 구현이 아니다.");
		assertThat(markdown).contains("RecommendationPresentation은 REST API가 아니다.");
		assertThat(markdown).contains("RecommendationPresentation은 React component가 아니다.");
		assertThat(markdown).contains("RecommendationPresentation은 approval request가 아니다.");
		assertThat(markdown).contains("RecommendationPresentation은 ActionCommand가 아니다.");
		assertThat(markdown).contains("RecommendationPresentation은 execution permission이 아니다.");
		assertThat(markdown).contains("RecommendationPresentation은 RECOMMENDATION_READY RecommendationModelIntegration에 의존한다.");
		assertThat(markdown).contains("scenario reference는 필수이다.");
		assertThat(markdown).contains("runbook reference는 필수이다.");
		assertThat(markdown).contains("rollback reference는 필수이다.");
		assertThat(markdown).contains("verification reference는 필수이다.");
		assertThat(markdown).contains("evidence reference는 필수이다.");
		assertThat(markdown).contains("payment safety classification은 필수이다.");
		assertThat(markdown).contains("RecommendationPresentationIntegration은 operator exposure readiness 해석 계층이다.");
		assertThat(markdown).contains("PRESENTABLE presentation만 EXPOSABLE 후보가 될 수 있다.");
		assertThat(markdown).contains("EXPOSABLE은 실제 UI/API 노출이 아니다.");
		assertThat(markdown).contains("payment safety uncertainty는 exposure BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 exposure BLOCKED이다.");
		assertThat(markdown).contains("raw payload 노출은 금지된다.");
		assertThat(markdown).contains("vendor detail 노출은 금지된다.");
		assertThat(markdown).contains("credential 노출은 금지된다.");
		assertThat(markdown).contains("configuration secret 노출은 금지된다.");
		assertThat(markdown).contains("RecommendationPresentationIntegration은 approval authority가 아니다.");
		assertThat(markdown).contains("RecommendationPresentationIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("RecommendationPresentationIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Recommendation Presentation");
		assertThat(markdown).contains("UI");
		assertThat(markdown).contains("React Component");
		assertThat(markdown).contains("REST API");
		assertThat(markdown).contains("SRE Console");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("Execution Authority");

		assertThat(markdown).contains("Actual UI Rendering");
		assertThat(markdown).contains("React Component Integration");
		assertThat(markdown).contains("REST API Exposure");
		assertThat(markdown).contains("SRE Console Integration");
		assertThat(markdown).contains("Approval Request Generation");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Recommendation Audit History");
		assertThat(markdown).contains("Recommendation Presentation Analytics");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Recommendation Presentation Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-recommendation-presentation-phase-closure.md"
		);
	}
}
