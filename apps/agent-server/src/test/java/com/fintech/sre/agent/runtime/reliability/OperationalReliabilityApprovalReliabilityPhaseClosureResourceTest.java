package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityApprovalReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityApprovalReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-approval-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Approval Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Approval Reliability Semantics");
		assertThat(markdown).contains("## 4. Recommendation Reliability Dependency");
		assertThat(markdown).contains(
				"## 5. Operator Context / Approval / Rollback / Verification Requirements"
		);
		assertThat(markdown).contains(
				"## 6. Approval Reliability Integration Semantics"
		);
		assertThat(markdown).contains(
				"## 7. Payment Safety / Contradiction Propagation"
		);
		assertThat(markdown).contains(
				"## 8. Operator-Facing Approval Boundary"
		);
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("ApprovalReliability");
		assertThat(markdown).contains("ApprovalReliabilityEvaluator");
		assertThat(markdown).contains("ApprovalReliabilityLevel");
		assertThat(markdown).contains("ApprovalReliabilityReason");
		assertThat(markdown).contains("ApprovalReliabilityScope");
		assertThat(markdown).contains("ApprovalReliabilityIntegration");
		assertThat(markdown).contains("ApprovalReliabilityIntegrationResult");
		assertThat(markdown).contains("ApprovalReliabilityIntegrationStatus");
		assertThat(markdown).contains("ApprovalReliabilityIntegrationReason");
		assertThat(markdown).contains("ApprovalReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"ApprovalReliability는 RecommendationReliability 위의 approval-readiness 신뢰도"
		);
		assertThat(markdown).contains("ApprovalReliability는 read-only");
		assertThat(markdown).contains("ApprovalReliability는 approval mutation이 아님");
		assertThat(markdown).contains("ApprovalReliability는 실제 approval 생성이 아님");
		assertThat(markdown).contains("ApprovalReliability는 approval request 생성이 아님");
		assertThat(markdown).contains("ApprovalReliability는 approval workflow 구현이 아님");
		assertThat(markdown).contains("ApprovalReliability는 execution permission이 아님");
		assertThat(markdown).contains("ApprovalReliability는 ActionCommand admission이 아님");
		assertThat(markdown).contains("BLOCKED recommendation reliability → approval BLOCKED");
		assertThat(markdown).contains("UNRELIABLE recommendation reliability → approval UNRELIABLE");
		assertThat(markdown).contains("LOW recommendation reliability → approval downgrade");
		assertThat(markdown).contains("missing human approval requirement → approval BLOCKED");
		assertThat(markdown).contains("missing operator context → approval BLOCKED");
		assertThat(markdown).contains("missing rollback binding → approval BLOCKED");
		assertThat(markdown).contains("missing verification binding → approval BLOCKED");
		assertThat(markdown).contains("payment safety uncertainty → approval downgrade");
		assertThat(markdown).contains("payment safety uncertainty → lifecycle CRITICAL risk 유지");
		assertThat(markdown).contains("contradictory recommendation/approval → lifecycle uncertainty 전파");
		assertThat(markdown).contains("BLOCKED approval reliability는 approval request 금지");
		assertThat(markdown).contains("UNRELIABLE approval reliability는 approval certainty 금지");
		assertThat(markdown).contains(
				"HIGH approval reliability는 HIGH recommendation reliability + human approval required + operator context + rollback binding + verification binding + no payment uncertainty + no contradiction 필요"
		);
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persisted approval reliability history");
		assertThat(markdown).contains("approval reliability trend analysis");
		assertThat(markdown).contains("policy-configurable approval reliability rules");
		assertThat(markdown).contains("SRE Console approval readiness visualization");
		assertThat(markdown).contains("Actual Approval Workflow integration");
		assertThat(markdown).contains("Verification Reliability");
		assertThat(markdown).contains("Action Admission integration");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Approval Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-approval-reliability-phase-closure.md"
		);
	}
}
