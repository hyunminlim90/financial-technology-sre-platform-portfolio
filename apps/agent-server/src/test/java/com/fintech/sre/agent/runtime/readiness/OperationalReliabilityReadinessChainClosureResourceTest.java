package com.fintech.sre.agent.runtime.readiness;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityReadinessChainClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityReadinessChainClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-readiness-chain-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Readiness Chain Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Runtime Readiness Chain Overview");
		assertThat(markdown).contains("## 3. Recommendation Readiness Layer");
		assertThat(markdown).contains("## 4. Approval Readiness Layer");
		assertThat(markdown).contains("## 5. Verification Readiness Layer");
		assertThat(markdown).contains("## 6. Action Admission Readiness Layer");
		assertThat(markdown).contains("## 7. Cross-Readiness Invariants");
		assertThat(markdown).contains("## 8. Lifecycle Risk Propagation Model");
		assertThat(markdown).contains(
				"## 9. Readiness Escalation And Downgrade Rules"
		);
		assertThat(markdown).contains("## 10. Runtime Boundaries");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains(
				"## 13. Runtime Readiness Chain Closure Summary"
		);

		assertThat(markdown).contains(
				"RecommendationReadiness\n        ↓\nApprovalReadiness\n        ↓\nVerificationReadiness\n        ↓\nActionAdmissionReadiness"
		);

		assertThat(markdown).contains("RecommendationReadiness");
		assertThat(markdown).contains("ApprovalReadiness");
		assertThat(markdown).contains("VerificationReadiness");
		assertThat(markdown).contains("ActionAdmissionReadiness");

		assertThat(markdown).contains("전체 readiness chain은 read-only runtime readiness model이다.");
		assertThat(markdown).contains("전체 readiness chain은 recommendation을 생성하지 않는다.");
		assertThat(markdown).contains("전체 readiness chain은 approval request를 생성하지 않는다.");
		assertThat(markdown).contains("전체 readiness chain은 verification request를 생성하지 않는다.");
		assertThat(markdown).contains("전체 readiness chain은 ActionCommand를 생성하지 않는다.");
		assertThat(markdown).contains("전체 readiness chain은 execution permission을 생성하지 않는다.");
		assertThat(markdown).contains("각 readiness 단계는 직전 readiness layer를 dependency로 사용한다.");
		assertThat(markdown).contains("lifecycle CRITICAL risk는 readiness 전 구간에서 BLOCKED를 유발한다.");
		assertThat(markdown).contains("payment safety uncertainty는 readiness 전 구간에서 BLOCKED를 유발한다.");
		assertThat(markdown).contains("lifecycle uncertainty는 readiness 전 구간에서 PARTIAL을 유발한다.");
		assertThat(markdown).contains("READY 상태는 상위 readiness와 필수 binding/context가 모두 만족될 때만 가능하다.");
		assertThat(markdown).contains("readiness completion은 execution authority가 아니다.");
		assertThat(markdown).contains("readiness completion은 action admission 결과가 아니다.");
		assertThat(markdown).contains("readiness completion은 recommendation 생성이 아니다.");
		assertThat(markdown).contains("readiness completion은 approval 생성이 아니다.");
		assertThat(markdown).contains("readiness completion은 verification 생성이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Runtime Readiness Chain");
		assertThat(markdown).contains("Recommendation Engine");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("Verification Workflow");
		assertThat(markdown).contains("Action Admission Engine");
		assertThat(markdown).contains("Execution Authority");
		assertThat(markdown).contains("Diagnostic Agent");

		assertThat(markdown).contains("Actual Recommendation Engine");
		assertThat(markdown).contains("Actual Approval Workflow");
		assertThat(markdown).contains("Actual Verification Workflow");
		assertThat(markdown).contains("Actual Action Admission Engine");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Diagnostic Agent Integration");
		assertThat(markdown).contains("eBPF/Perf Diagnostic Integration");
		assertThat(markdown).contains("Incident Closure Integration");
		assertThat(markdown).contains("SRE Console Visualization");
		assertThat(markdown).contains("Readiness Trend Analytics");
		assertThat(markdown).contains("Readiness History Persistence");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Readiness Chain Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-readiness-chain-closure.md"
		);
	}
}
