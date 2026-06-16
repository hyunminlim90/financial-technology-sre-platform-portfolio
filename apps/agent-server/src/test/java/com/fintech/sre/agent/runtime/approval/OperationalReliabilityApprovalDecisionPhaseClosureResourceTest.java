package com.fintech.sre.agent.runtime.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityApprovalDecisionPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalApprovalDecisionPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-approval-decision-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Approval Decision Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Approval Decision Semantics");
		assertThat(markdown).contains("## 4. Approval State Dependency");
		assertThat(markdown).contains(
				"## 5. Required Approval Decision Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Approval Decision Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Decision Pending Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ApprovalDecision");
		assertThat(markdown).contains("ApprovalDecisionEvaluator");
		assertThat(markdown).contains("ApprovalDecisionLevel");
		assertThat(markdown).contains("ApprovalDecisionReason");
		assertThat(markdown).contains("ApprovalDecisionScope");
		assertThat(markdown).contains("ApprovalDecisionIntegration");
		assertThat(markdown).contains("ApprovalDecisionIntegrationResult");
		assertThat(markdown).contains("ApprovalDecisionIntegrationStatus");
		assertThat(markdown).contains("ApprovalDecisionIntegrationReason");
		assertThat(markdown).contains("ApprovalDecisionIntegrationScope");

		assertThat(markdown).contains("ApprovalDecision은 승인 의사결정 상태 표현 계층이다.");
		assertThat(markdown).contains("ApprovalDecision은 read-only이다.");
		assertThat(markdown).contains("ApprovalDecision은 human approval이 아니다.");
		assertThat(markdown).contains("ApprovalDecision은 approval result가 아니다.");
		assertThat(markdown).contains("ApprovalDecision은 approval workflow가 아니다.");
		assertThat(markdown).contains("ApprovalDecision은 verification request가 아니다.");
		assertThat(markdown).contains("ApprovalDecision은 ActionCommand가 아니다.");
		assertThat(markdown).contains("ApprovalDecision은 execution permission이 아니다.");
		assertThat(markdown).contains("ApprovalDecision은 ApprovalStateIntegration에 의존한다.");
		assertThat(markdown).contains("DECISION_PENDING만 approval decision 후보가 될 수 있다.");
		assertThat(markdown).contains("decisionIdentifier는 필수이다.");
		assertThat(markdown).contains("approvalPolicy는 필수이다.");
		assertThat(markdown).contains("operatorContext는 필수이다.");
		assertThat(markdown).contains("decisionRationaleRequirement는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ApprovalDecisionIntegration은 decision pending view 해석 계층이다.");
		assertThat(markdown).contains("APPROVAL_DECISION_PENDING_VIEW는 실제 approval result가 아니다.");
		assertThat(markdown).contains("ApprovalDecisionIntegration은 approval authority가 아니다.");
		assertThat(markdown).contains("ApprovalDecisionIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ApprovalDecisionIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Approval Decision");
		assertThat(markdown).contains("Human Approval");
		assertThat(markdown).contains("Approval Result");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("Verification Request");
		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("Execution Permission");

		assertThat(markdown).contains("Actual Human Approval");
		assertThat(markdown).contains("Approval Result Model");
		assertThat(markdown).contains("Approval Workflow Implementation");
		assertThat(markdown).contains("Approval Persistence");
		assertThat(markdown).contains("Approval Audit History");
		assertThat(markdown).contains("Verification Request Generation");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Approval Analytics");
		assertThat(markdown).contains("Human Override Analytics");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Approval Decision Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-approval-decision-phase-closure.md"
		);
	}
}
