package com.fintech.sre.agent.internal.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class InternalOperationalApiFilterTest {

	@Test
	void shouldIgnorePublicApiPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/recommendations").build()
		);

		WebFilterChain chain = e -> {
			e.getResponse().setStatusCode(HttpStatus.OK);
			return Mono.empty();
		};

		filter.filter(exchange, chain).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForAdminPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/admin/knowledge/ingest").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForAlertPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/internal/alerts/prometheus").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForRecommendationPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/recommendations/recent").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForIncidentPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/incidents/incident-1/approval/audit").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnForbiddenWhenSecretMissing() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(true, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/recommendations/recent").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForExecutionPlanPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/execution-plans/plan-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForExecutionResultPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/execution-results/result-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForVerificationResultPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/verification-results/verification-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForPostmortemDraftPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/postmortem-drafts/draft-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForLearningCandidatePath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/learning-candidates/candidate-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForKnowledgePromotionPlanPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/knowledge-promotion-plans/plan-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForKnowledgeUpdatePath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/knowledge-updates/update-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernancePath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/dashboard/summary").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceBacklogPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/dashboard/backlog").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceHealthPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/dashboard/health").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceSearchPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/search?q=incident").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceSearchHealthPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/search/health").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceConsoleHealthPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/console/health").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceConsoleRuntimeSummaryPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/console/runtime-summary").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceDetailHealthPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/details/health").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceDetailOverviewPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get(
						"/internal/governance/details/overview/incidents/incident-1"
				).build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceDetailPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/details/incidents/incident-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceLearningDetailPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/details/learning-candidates/candidate-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceKnowledgeUpdateDetailPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/details/knowledge-updates/update-1").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceTimelinePath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/timeline").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceTimelineHealthPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/governance/timeline/health").build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnNotFoundWhenDisabledForGovernanceTimelineRuntimeSummaryPath() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(false, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get(
						"/internal/governance/timeline/runtime-summary"
				).build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldAllowWhenHeaderMatches() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(true, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/recommendations/recent")
						.header("X-FIN-SRE-INTERNAL", "secret")
						.build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void shouldAllowWhenBearerTokenMatches() {
		InternalOperationalApiFilter filter = new InternalOperationalApiFilter(
				properties(true, "secret")
		);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/internal/recommendations/recent")
						.header("Authorization", "Bearer secret")
						.build()
		);

		filter.filter(exchange, passChain()).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	private InternalOperationalApiSecurityProperties properties(
			boolean enabled,
			String secret
	) {
		return new InternalOperationalApiSecurityProperties(
				enabled,
				true,
				"X-FIN-SRE-INTERNAL",
				secret,
				List.of(
						"/internal/admin/",
						"/internal/alerts/",
						"/internal/recommendations/",
						"/internal/incidents/",
						"/internal/execution-plans/",
						"/internal/execution-results/",
						"/internal/verification-results/",
						"/internal/postmortem-drafts/",
						"/internal/learning-candidates/",
						"/internal/knowledge-promotion-plans/",
						"/internal/knowledge-updates/",
						"/internal/governance/"
				)
		);
	}

	private WebFilterChain passChain() {
		return e -> {
			e.getResponse().setStatusCode(HttpStatus.OK);
			return Mono.empty();
		};
	}
}
