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
class GovernanceDashboardQueryRepositoryTest {

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
	void shouldAggregateStatusCountsWithinTimeRange() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		GovernanceDashboardTimeRange range =
				new GovernanceDashboardTimeRange(base, base.plusSeconds(4 * 60 * 60));

		approvalStore.save(approval("approval-a", "incident-a", "rec-a",
				RecommendationApprovalStatus.APPROVED, base.plusSeconds(10))).block();
		approvalStore.save(approval("approval-b", "incident-a", "rec-b",
				RecommendationApprovalStatus.APPROVED, base.plusSeconds(20))).block();
		approvalStore.save(approval("approval-c", "incident-b", "rec-c",
				RecommendationApprovalStatus.REJECTED, base.plusSeconds(30))).block();
		approvalStore.save(approval("approval-d", "incident-z", "rec-z",
				RecommendationApprovalStatus.PENDING, base.minusSeconds(60))).block();

		verificationStore.save(verification("verification-a", "incident-a", "result-a",
				VerificationStatus.VERIFIED, base.plusSeconds(40))).block();
		verificationStore.save(verification("verification-b", "incident-b", "result-b",
				VerificationStatus.REGRESSION_DETECTED, base.plusSeconds(50))).block();
		verificationStore.save(verification("verification-c", "incident-z", "result-z",
				VerificationStatus.NOT_VERIFIED, base.minusSeconds(60))).block();

		List<GovernanceDashboardQueryResult> approvalSummary =
				queryRepository.findApprovalStatusSummary(range).collectList().block();
		List<GovernanceDashboardQueryResult> verificationSummary =
				queryRepository.findVerificationStatusSummary(range).collectList().block();

		assertThat(approvalSummary)
				.containsExactly(
						new GovernanceDashboardQueryResult("APPROVED", 2L),
						new GovernanceDashboardQueryResult("REJECTED", 1L)
				);
		assertThat(verificationSummary)
				.containsExactlyInAnyOrder(
						new GovernanceDashboardQueryResult("VERIFIED", 1L),
						new GovernanceDashboardQueryResult("REGRESSION_DETECTED", 1L)
				);
	}

	@Test
	void shouldAggregateLatestIncidentStatusPerIncidentWithinTimeRange() {
		Instant base = Instant.parse("2026-05-11T00:00:00Z");
		GovernanceDashboardTimeRange range =
				new GovernanceDashboardTimeRange(base, base.plusSeconds(3 * 60 * 60));

		incidentLifecycleStore.save(lifecycle("lifecycle-a-open", "incident-a",
				null, IncidentStatus.OPEN, base.plusSeconds(10))).block();
		incidentLifecycleStore.save(lifecycle("lifecycle-a-resolved", "incident-a",
				IncidentStatus.STABILIZING, IncidentStatus.RESOLVED, base.plusSeconds(100))).block();
		incidentLifecycleStore.save(lifecycle("lifecycle-b-open", "incident-b",
				null, IncidentStatus.OPEN, base.plusSeconds(20))).block();
		incidentLifecycleStore.save(lifecycle("lifecycle-b-mitigating", "incident-b",
				IncidentStatus.OPEN, IncidentStatus.MITIGATING, base.plusSeconds(200))).block();
		incidentLifecycleStore.save(lifecycle("lifecycle-c-outside", "incident-c",
				null, IncidentStatus.ESCALATED, base.minusSeconds(20))).block();

		List<GovernanceDashboardQueryResult> latestStatuses =
				queryRepository.findLatestIncidentStatusSummary(range).collectList().block();

		assertThat(latestStatuses)
				.containsExactlyInAnyOrder(
						new GovernanceDashboardQueryResult("MITIGATING", 1L),
						new GovernanceDashboardQueryResult("RESOLVED", 1L)
				);
	}

	@Test
	void shouldReturnApprovalBucketsInDescendingOrder() {
		Instant base = Instant.parse("2026-05-12T00:00:00Z");
		GovernanceDashboardTimeRange range =
				new GovernanceDashboardTimeRange(base, base.plusSeconds(3 * 60 * 60));

		approvalStore.save(approval("approval-hour-1", "incident-hour-1", "rec-hour-1",
				RecommendationApprovalStatus.APPROVED, base.plusSeconds(70 * 60))).block();
		approvalStore.save(approval("approval-hour-2", "incident-hour-2", "rec-hour-2",
				RecommendationApprovalStatus.REJECTED, base.plusSeconds(20 * 60))).block();

		List<GovernanceDashboardTimeBucketResult> buckets =
				queryRepository.findApprovalStatusBuckets(range, GovernanceDashboardBucketSize.ONE_HOUR)
						.collectList()
						.block();

		assertThat(buckets).isNotEmpty();
		assertThat(buckets.get(0).bucketStart()).isAfterOrEqualTo(buckets.get(1).bucketStart());
		assertThat(buckets)
				.anySatisfy(result -> {
					assertThat(result.name()).isEqualTo("APPROVED");
					assertThat(result.count()).isEqualTo(1L);
				});
	}

	@Test
	void shouldReturnEmptyResultsWhenNoRecordsMatchTimeRange() {
		GovernanceDashboardTimeRange range = new GovernanceDashboardTimeRange(
				Instant.parse("2026-06-01T00:00:00Z"),
				Instant.parse("2026-06-01T01:00:00Z")
		);

		assertThat(queryRepository.findApprovalStatusSummary(range).collectList().block()).isEmpty();
		assertThat(queryRepository.findVerificationStatusSummary(range).collectList().block()).isEmpty();
		assertThat(queryRepository.findLatestIncidentStatusSummary(range).collectList().block()).isEmpty();
	}

	private RecommendationApprovalRecord approval(
			String approvalId,
			String incidentId,
			String recommendationRecordId,
			RecommendationApprovalStatus status,
			Instant decidedAt
	) {
		return new RecommendationApprovalRecord(
				approvalId,
				recommendationRecordId,
				incidentId,
				status,
				"operator",
				"reason",
				decidedAt,
				Map.of()
		);
	}

	private VerificationResultRecord verification(
			String verificationId,
			String incidentId,
			String executionResultId,
			VerificationStatus status,
			Instant verifiedAt
	) {
		return new VerificationResultRecord(
				verificationId,
				executionResultId,
				"plan-" + verificationId,
				"rec-" + verificationId,
				incidentId,
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
			IncidentStatus previousStatus,
			IncidentStatus currentStatus,
			Instant transitionedAt
	) {
		return new IncidentLifecycleRecord(
				lifecycleId,
				incidentId,
				previousStatus,
				currentStatus,
				IncidentTransitionReason.MANUAL_ESCALATION,
				"operator",
				"summary",
				transitionedAt,
				Map.of()
		);
	}
}
