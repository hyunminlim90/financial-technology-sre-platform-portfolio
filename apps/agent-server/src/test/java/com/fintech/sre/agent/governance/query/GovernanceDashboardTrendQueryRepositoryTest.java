package com.fintech.sre.agent.governance.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardBucketSize;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardTimeRange;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("r2dbc")
@Testcontainers(disabledWithoutDocker = true)
class GovernanceDashboardTrendQueryRepositoryTest {

	@Container
	@SuppressWarnings("resource")
	static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("fin_sre")
					.withUsername("fin_sre")
					.withPassword("fin_sre")
					.withInitScript("db/schema-governance.sql");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add(
				"spring.r2dbc.url",
				() -> "r2dbc:postgresql://"
						+ POSTGRES.getHost()
						+ ":"
						+ POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
						+ "/"
						+ POSTGRES.getDatabaseName()
		);
		registry.add("spring.r2dbc.username", POSTGRES::getUsername);
		registry.add("spring.r2dbc.password", POSTGRES::getPassword);
	}

	@Autowired
	private GovernanceDashboardQueryRepository queryRepository;

	@Autowired
	private RecommendationApprovalStore approvalStore;

	@Autowired
	private VerificationResultStore verificationStore;

	@Autowired
	private IncidentLifecycleStore incidentLifecycleStore;

	@Test
	void shouldAggregateApprovalAndVerificationTrendsByBucket() {
		Instant base = Instant.parse("2026-05-12T00:00:00Z");
		GovernanceDashboardTimeRange range =
				new GovernanceDashboardTimeRange(base, base.plusSeconds(3 * 60 * 60));

		approvalStore.save(approval("approval-a", RecommendationApprovalStatus.APPROVED,
				base.plusSeconds(10 * 60))).block();
		approvalStore.save(approval("approval-b", RecommendationApprovalStatus.REJECTED,
				base.plusSeconds(70 * 60))).block();
		approvalStore.save(approval("approval-c", RecommendationApprovalStatus.APPROVED,
				base.plusSeconds(75 * 60))).block();

		verificationStore.save(verification("verification-a", VerificationStatus.VERIFIED,
				base.plusSeconds(20 * 60))).block();
		verificationStore.save(verification("verification-b", VerificationStatus.REGRESSION_DETECTED,
				base.plusSeconds(80 * 60))).block();

		List<GovernanceDashboardTimeBucketResult> approvalBuckets =
				queryRepository.findApprovalStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
						.collectList()
						.block();
		List<GovernanceDashboardTimeBucketResult> verificationBuckets =
				queryRepository.findVerificationStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
						.collectList()
						.block();

		assertThat(approvalBuckets)
				.contains(
						new GovernanceDashboardTimeBucketResult(base, "APPROVED", 1L),
						new GovernanceDashboardTimeBucketResult(base.plusSeconds(60 * 60), "APPROVED", 1L),
						new GovernanceDashboardTimeBucketResult(base.plusSeconds(60 * 60), "REJECTED", 1L)
				);
		assertThat(verificationBuckets)
				.contains(
						new GovernanceDashboardTimeBucketResult(base, "VERIFIED", 1L),
						new GovernanceDashboardTimeBucketResult(base.plusSeconds(60 * 60),
								"REGRESSION_DETECTED", 1L)
				);
	}

	@Test
	void shouldAggregateIncidentLifecycleTransitionsByBucket() {
		Instant base = Instant.parse("2026-05-13T00:00:00Z");
		GovernanceDashboardTimeRange range =
				new GovernanceDashboardTimeRange(base, base.plusSeconds(3 * 60 * 60));

		incidentLifecycleStore.save(lifecycle("lifecycle-a", "incident-a", IncidentStatus.OPEN,
				base.plusSeconds(15 * 60))).block();
		incidentLifecycleStore.save(lifecycle("lifecycle-b", "incident-a", IncidentStatus.RESOLVED,
				base.plusSeconds(85 * 60))).block();
		incidentLifecycleStore.save(lifecycle("lifecycle-c", "incident-b", IncidentStatus.MITIGATING,
				base.plusSeconds(95 * 60))).block();

		List<GovernanceDashboardTimeBucketResult> lifecycleBuckets =
				queryRepository.findIncidentLifecycleStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
						.collectList()
						.block();

		assertThat(lifecycleBuckets)
				.contains(
						new GovernanceDashboardTimeBucketResult(base, "OPEN", 1L),
						new GovernanceDashboardTimeBucketResult(base.plusSeconds(60 * 60), "MITIGATING", 1L),
						new GovernanceDashboardTimeBucketResult(base.plusSeconds(60 * 60), "RESOLVED", 1L)
				);
	}

	@Test
	void shouldReturnEmptyTrendResultsWhenNoRowsMatch() {
		GovernanceDashboardTimeRange range = new GovernanceDashboardTimeRange(
				Instant.parse("2026-06-10T00:00:00Z"),
				Instant.parse("2026-06-10T02:00:00Z")
		);

		assertThat(queryRepository.findApprovalStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
				.collectList().block()).isEmpty();
		assertThat(queryRepository.findVerificationStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
				.collectList().block()).isEmpty();
		assertThat(queryRepository.findIncidentLifecycleStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
				.collectList().block()).isEmpty();
	}

	private RecommendationApprovalRecord approval(
			String approvalId,
			RecommendationApprovalStatus status,
			Instant decidedAt
	) {
		return new RecommendationApprovalRecord(
				approvalId,
				"rec-" + approvalId,
				"incident-" + approvalId,
				status,
				"operator",
				"reason",
				decidedAt,
				Map.of()
		);
	}

	private VerificationResultRecord verification(
			String verificationId,
			VerificationStatus status,
			Instant verifiedAt
	) {
		return new VerificationResultRecord(
				verificationId,
				"result-" + verificationId,
				"plan-" + verificationId,
				"rec-" + verificationId,
				"incident-" + verificationId,
				status,
				"operator",
				"summary",
				verifiedAt,
				Map.of()
		);
	}

	private IncidentLifecycleRecord lifecycle(
			String lifecycleId,
			String incidentId,
			IncidentStatus currentStatus,
			Instant transitionedAt
	) {
		return new IncidentLifecycleRecord(
				lifecycleId,
				incidentId,
				currentStatus == IncidentStatus.OPEN ? null : IncidentStatus.OPEN,
				currentStatus,
				IncidentTransitionReason.MITIGATION_IN_PROGRESS,
				"operator",
				"summary",
				transitionedAt,
				Map.of()
		);
	}
}
