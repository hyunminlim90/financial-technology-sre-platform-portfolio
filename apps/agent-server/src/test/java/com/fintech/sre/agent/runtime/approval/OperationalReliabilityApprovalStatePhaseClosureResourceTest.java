package com.fintech.sre.agent.runtime.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityApprovalStatePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalApprovalStatePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-approval-state-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Approval State Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Approval State Semantics");
		assertThat(markdown).contains("## 4. Approval Request Dependency");
		assertThat(markdown).contains(
				"## 5. Required Approval State Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Approval State Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Pending Approval Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ApprovalState");
		assertThat(markdown).contains("ApprovalStateEvaluator");
		assertThat(markdown).contains("ApprovalStateLevel");
		assertThat(markdown).contains("ApprovalStateReason");
		assertThat(markdown).contains("ApprovalStateScope");
		assertThat(markdown).contains("ApprovalStateIntegration");
		assertThat(markdown).contains("ApprovalStateIntegrationResult");
		assertThat(markdown).contains("ApprovalStateIntegrationStatus");
		assertThat(markdown).contains("ApprovalStateIntegrationReason");
		assertThat(markdown).contains("ApprovalStateIntegrationScope");

		assertThat(markdown).contains("ApprovalState는 approval 상태 표현 계층이다.");
		assertThat(markdown).contains("ApprovalState는 read-only이다.");
		assertThat(markdown).contains("ApprovalState는 human approval이 아니다.");
		assertThat(markdown).contains("ApprovalState는 approval decision이 아니다.");
		assertThat(markdown).contains("ApprovalState는 approval workflow가 아니다.");
		assertThat(markdown).contains("ApprovalState는 ActionCommand가 아니다.");
		assertThat(markdown).contains("ApprovalState는 execution permission이 아니다.");
		assertThat(markdown).contains("ApprovalState는 ApprovalRequestIntegration에 의존한다.");
		assertThat(markdown).contains("PENDING_APPROVAL만 approval state 후보가 될 수 있다.");
		assertThat(markdown).contains("approvalStateIdentifier는 필수이다.");
		assertThat(markdown).contains("approvalPolicy는 필수이다.");
		assertThat(markdown).contains("operatorContext는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ApprovalStateIntegration은 pending approval view 해석 계층이다.");
		assertThat(markdown).contains("APPROVAL_PENDING_VIEW는 실제 human approval이 아니다.");
		assertThat(markdown).contains("ApprovalStateIntegration은 approval authority가 아니다.");
		assertThat(markdown).contains("ApprovalStateIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ApprovalStateIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Approval State");
		assertThat(markdown).contains("Human Approval");
		assertThat(markdown).contains("Approval Decision");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("Verification Request");
		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("Execution Permission");

		assertThat(markdown).contains("Actual Human Approval");
		assertThat(markdown).contains("Approval Decision Model");
		assertThat(markdown).contains("Approval Workflow Implementation");
		assertThat(markdown).contains("Approval Persistence");
		assertThat(markdown).contains("Approval Audit History");
		assertThat(markdown).contains("Approval Notification");
		assertThat(markdown).contains("Verification Request Generation");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Approval Analytics");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Approval State Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-approval-state-phase-closure.md"
		);
	}
}
