package com.fintech.sre.agent.runtime.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityApprovalRequestPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalApprovalRequestPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-approval-request-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Approval Request Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Approval Request Semantics");
		assertThat(markdown).contains(
				"## 4. Recommendation Presentation Dependency"
		);
		assertThat(markdown).contains(
				"## 5. Required Approval Request Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Approval Request Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Approval Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ApprovalRequest");
		assertThat(markdown).contains("ApprovalRequestEvaluator");
		assertThat(markdown).contains("ApprovalRequestLevel");
		assertThat(markdown).contains("ApprovalRequestReason");
		assertThat(markdown).contains("ApprovalRequestScope");
		assertThat(markdown).contains("ApprovalRequestIntegration");
		assertThat(markdown).contains("ApprovalRequestIntegrationResult");
		assertThat(markdown).contains("ApprovalRequestIntegrationStatus");
		assertThat(markdown).contains("ApprovalRequestIntegrationReason");
		assertThat(markdown).contains("ApprovalRequestIntegrationScope");

		assertThat(markdown).contains("ApprovalRequest는 Approval Workflow 진입 가능 상태 평가 계층이다.");
		assertThat(markdown).contains("ApprovalRequest는 read-only이다.");
		assertThat(markdown).contains("ApprovalRequest는 actual approval request가 아니다.");
		assertThat(markdown).contains("ApprovalRequest는 human approval이 아니다.");
		assertThat(markdown).contains("ApprovalRequest는 approval workflow가 아니다.");
		assertThat(markdown).contains("ApprovalRequest는 ActionCommand가 아니다.");
		assertThat(markdown).contains("ApprovalRequest는 execution permission이 아니다.");
		assertThat(markdown).contains("ApprovalRequest는 RecommendationPresentationIntegration에 의존한다.");
		assertThat(markdown).contains("REQUESTABLE만 approval request 후보가 될 수 있다.");
		assertThat(markdown).contains("operator context는 필수이다.");
		assertThat(markdown).contains("human approval requirement는 필수이다.");
		assertThat(markdown).contains("approval policy는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ApprovalRequestIntegration은 approval request readiness 해석 계층이다.");
		assertThat(markdown).contains("APPROVAL_REQUEST_READY는 실제 approval request 생성이 아니다.");
		assertThat(markdown).contains("ApprovalRequestIntegration은 approval authority가 아니다.");
		assertThat(markdown).contains("ApprovalRequestIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ApprovalRequestIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Approval Request");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("Human Approval");
		assertThat(markdown).contains("Approval State");
		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("SRE Console Approval UI");

		assertThat(markdown).contains("Actual Approval Request Generation");
		assertThat(markdown).contains("Approval Workflow Implementation");
		assertThat(markdown).contains("Human Approval State Model");
		assertThat(markdown).contains("Approval Audit History");
		assertThat(markdown).contains("Approval Persistence");
		assertThat(markdown).contains("Approval API Exposure");
		assertThat(markdown).contains("Approval Notification");
		assertThat(markdown).contains("SRE Console Approval UI");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Approval Analytics");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Approval Request Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-approval-request-phase-closure.md"
		);
	}
}
