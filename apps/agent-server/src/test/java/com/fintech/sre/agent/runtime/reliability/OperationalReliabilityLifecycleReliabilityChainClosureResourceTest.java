package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityLifecycleReliabilityChainClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityLifecycleReliabilityChainClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-lifecycle-reliability-chain-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Lifecycle Reliability Chain Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Lifecycle Reliability Chain Overview");
		assertThat(markdown).contains("## 3. Evidence Reliability Layer");
		assertThat(markdown).contains("## 4. Assessment Reliability Layer");
		assertThat(markdown).contains("## 5. Decision Reliability Layer");
		assertThat(markdown).contains("## 6. Recommendation Reliability Layer");
		assertThat(markdown).contains("## 7. Approval Reliability Layer");
		assertThat(markdown).contains("## 8. Verification Reliability Layer");
		assertThat(markdown).contains("## 9. Action Admission Reliability Layer");
		assertThat(markdown).contains("## 10. Cross-Lifecycle Reliability Invariants");
		assertThat(markdown).contains("## 11. Payment Safety Propagation Model");
		assertThat(markdown).contains(
				"## 12. Reliability Escalation And Downgrade Rules"
		);
		assertThat(markdown).contains("## 13. Runtime Boundaries");
		assertThat(markdown).contains("## 14. Deferred Scope");
		assertThat(markdown).contains("## 15. Non-Goals");
		assertThat(markdown).contains(
				"## 16. Lifecycle Reliability Chain Closure Summary"
		);

		assertThat(markdown).contains(
				"EvidenceReliability\n        ↓\nAssessmentReliability\n        ↓\nDecisionReliability\n        ↓\nRecommendationReliability\n        ↓\nApprovalReliability\n        ↓\nVerificationReliability\n        ↓\nActionAdmissionReliability"
		);

		assertThat(markdown).contains("EvidenceReliability");
		assertThat(markdown).contains("AssessmentReliability");
		assertThat(markdown).contains("DecisionReliability");
		assertThat(markdown).contains("RecommendationReliability");
		assertThat(markdown).contains("ApprovalReliability");
		assertThat(markdown).contains("VerificationReliability");
		assertThat(markdown).contains("ActionAdmissionReliability");

		assertThat(markdown).contains("전체 chain은 read-only semantic reliability model이다.");
		assertThat(markdown).contains("전체 chain은 recommendation을 생성하지 않는다.");
		assertThat(markdown).contains("전체 chain은 approval을 생성하지 않는다.");
		assertThat(markdown).contains("전체 chain은 verification을 생성하지 않는다.");
		assertThat(markdown).contains("전체 chain은 ActionCommand를 생성하지 않는다.");
		assertThat(markdown).contains("전체 chain은 execution permission을 생성하지 않는다.");
		assertThat(markdown).contains("전체 chain은 incident closure를 수행하지 않는다.");
		assertThat(markdown).contains("각 단계는 직전 reliability layer를 dependency로 사용한다.");
		assertThat(markdown).contains("HIGH reliability는 상위 dependency와 required binding이 모두 만족될 때만 가능하다.");
		assertThat(markdown).contains("BLOCKED 상태는 downstream layer에 전파될 수 있다.");
		assertThat(markdown).contains("UNRELIABLE 상태는 downstream layer에 전파될 수 있다.");
		assertThat(markdown).contains("payment safety uncertainty는 chain 전반에서 CRITICAL risk로 유지된다.");
		assertThat(markdown).contains("contradiction은 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("missing scenario binding은 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("missing rollback binding은 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("missing verification binding은 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("missing approval requirement는 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("missing action type은 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("missing blast radius boundary는 lifecycle uncertainty를 유발한다.");
		assertThat(markdown).contains("lifecycle reliability completion은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Lifecycle Reliability Chain");
		assertThat(markdown).contains("Recommendation Engine");
		assertThat(markdown).contains("Approval Workflow");
		assertThat(markdown).contains("Verification Workflow");
		assertThat(markdown).contains("Action Admission Engine");
		assertThat(markdown).contains("Execution Engine");
		assertThat(markdown).contains("Diagnostic Agent");

		assertThat(markdown).contains("Actual Recommendation Generation");
		assertThat(markdown).contains("Actual Approval Workflow");
		assertThat(markdown).contains("Actual Verification Workflow");
		assertThat(markdown).contains("Actual Action Admission");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Diagnostic Agent Integration");
		assertThat(markdown).contains("eBPF/Perf Diagnostic Integration");
		assertThat(markdown).contains("Incident Closure Integration");
		assertThat(markdown).contains("SRE Console Visualization");
		assertThat(markdown).contains("Reliability Trend Analytics");
		assertThat(markdown).contains("Reliability History Persistence");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Lifecycle Reliability Chain Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-lifecycle-reliability-chain-closure.md"
		);
	}
}
