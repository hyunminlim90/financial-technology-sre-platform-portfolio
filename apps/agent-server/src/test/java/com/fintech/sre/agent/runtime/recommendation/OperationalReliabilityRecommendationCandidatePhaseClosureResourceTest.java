package com.fintech.sre.agent.runtime.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationCandidatePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalRecommendationCandidatePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-recommendation-candidate-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Recommendation Candidate Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Recommendation Candidate Semantics");
		assertThat(markdown).contains("## 4. Action Admission Readiness Dependency");
		assertThat(markdown).contains("## 5. Required Binding Model");
		assertThat(markdown).contains(
				"## 6. Recommendation Candidate Integration Semantics"
		);
		assertThat(markdown).contains(
				"## 7. Payment Safety / Lifecycle Risk Boundary"
		);
		assertThat(markdown).contains("## 8. Runtime Boundaries");
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("RecommendationCandidate");
		assertThat(markdown).contains("RecommendationCandidateEvaluator");
		assertThat(markdown).contains("RecommendationCandidateLevel");
		assertThat(markdown).contains("RecommendationCandidateReason");
		assertThat(markdown).contains("RecommendationCandidateScope");
		assertThat(markdown).contains("RecommendationCandidateIntegration");
		assertThat(markdown).contains("RecommendationCandidateIntegrationResult");
		assertThat(markdown).contains("RecommendationCandidateIntegrationStatus");
		assertThat(markdown).contains("RecommendationCandidateIntegrationReason");
		assertThat(markdown).contains("RecommendationCandidateIntegrationScope");

		assertThat(markdown).contains("RecommendationCandidate는 Recommendation Engine 진입 전 candidate gate이다.");
		assertThat(markdown).contains("RecommendationCandidate는 read-only이다.");
		assertThat(markdown).contains("RecommendationCandidate는 actual recommendation이 아니다.");
		assertThat(markdown).contains("RecommendationCandidate는 recommendation content model이 아니다.");
		assertThat(markdown).contains("RecommendationCandidate는 runbook selection이 아니다.");
		assertThat(markdown).contains("RecommendationCandidate는 LLM/RAG 호출이 아니다.");
		assertThat(markdown).contains("RecommendationCandidate는 approval request가 아니다.");
		assertThat(markdown).contains("RecommendationCandidate는 ActionCommand가 아니다.");
		assertThat(markdown).contains("RecommendationCandidate는 execution permission이 아니다.");
		assertThat(markdown).contains("ActionAdmissionReadiness BLOCKED → candidate BLOCKED");
		assertThat(markdown).contains("ActionAdmissionReadiness UNRELIABLE → candidate UNRELIABLE");
		assertThat(markdown).contains("ActionAdmissionReadiness NOT_READY → candidate NOT_READY");
		assertThat(markdown).contains("ActionAdmissionReadiness PARTIAL → candidate PARTIAL");
		assertThat(markdown).contains("ActionAdmissionReadiness READY + required bindings → candidate ELIGIBLE");
		assertThat(markdown).contains("missing scenario binding → candidate BLOCKED");
		assertThat(markdown).contains("missing runbook binding → candidate BLOCKED");
		assertThat(markdown).contains("missing rollback binding → candidate BLOCKED");
		assertThat(markdown).contains("missing verification binding → candidate BLOCKED");
		assertThat(markdown).contains("payment safety uncertainty → candidate BLOCKED");
		assertThat(markdown).contains("critical lifecycle risk → candidate BLOCKED");
		assertThat(markdown).contains("ELIGIBLE candidate만 GENERATION_READY 해석 후보가 될 수 있다.");
		assertThat(markdown).contains("RecommendationCandidateIntegration은 generation-ready 해석 계층이다.");
		assertThat(markdown).contains("RecommendationCandidateIntegration은 recommendation 생성이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("actual recommendation generation");
		assertThat(markdown).contains("recommendation content model");
		assertThat(markdown).contains("runbook selection");
		assertThat(markdown).contains("RAG retrieval");
		assertThat(markdown).contains("LLM prompt/response");
		assertThat(markdown).contains("approval request generation");
		assertThat(markdown).contains("ActionCommand generation");
		assertThat(markdown).contains("execution permission");
		assertThat(markdown).contains("recommendation persistence");
		assertThat(markdown).contains("recommendation API exposure");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Recommendation Candidate Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-recommendation-candidate-phase-closure.md"
		);
	}
}
