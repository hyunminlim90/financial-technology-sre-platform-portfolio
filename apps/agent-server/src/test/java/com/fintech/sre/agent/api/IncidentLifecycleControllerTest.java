package com.fintech.sre.agent.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentLifecycleControllerTest {

	private final ApplicationContext applicationContext;
	private WebTestClient webTestClient;

	IncidentLifecycleControllerTest(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeEach
	void setUp() {
		this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).configureClient().build();
	}

	@Test
	void shouldCreateAndTransitionLifecycle() {
		webTestClient.post()
				.uri("/api/incidents/INC-LIFECYCLE-API/lifecycle")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.incidentId").isEqualTo("INC-LIFECYCLE-API")
				.jsonPath("$.status").isEqualTo("DETECTED");

		webTestClient.post()
				.uri("/api/incidents/INC-LIFECYCLE-API/lifecycle/transition")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "status": "RECOMMENDATION_CREATED",
						  "reason": "recommendation created"
						}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("RECOMMENDATION_CREATED");

		webTestClient.get()
				.uri("/api/incidents/INC-LIFECYCLE-API/lifecycle")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("RECOMMENDATION_CREATED");
	}

	@Test
	void shouldRejectInvalidTransitionWithStructuredError() {
		webTestClient.post()
				.uri("/api/incidents/INC-LIFECYCLE-ERROR/lifecycle")
				.exchange()
				.expectStatus().isOk();

		webTestClient.post()
				.uri("/api/incidents/INC-LIFECYCLE-ERROR/lifecycle/transition")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("""
						{
						  "status": "ACTION_APPROVED",
						  "reason": "skip ahead"
						}
						""")
				.exchange()
				.expectStatus().isEqualTo(409)
				.expectBody()
				.jsonPath("$.message").isEqualTo("Invalid state transition or invalid application state")
				.jsonPath("$.details[0].code").isEqualTo("INVALID_STATE")
				.jsonPath("$.details[0].severity").isEqualTo("ERROR")
				.jsonPath("$.humanActionRequired").isEqualTo("Human operator must review the workflow state.");
	}
}
