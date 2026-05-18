package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardBucketSize;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthMetricsRecorder;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthService;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardTimeRange;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthMetricsRecorder;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthService;
import com.fintech.sre.agent.governance.detail.GovernanceDetailResilienceProperties;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult;
import com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthMetricsRecorder;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthService;
import com.fintech.sre.agent.governance.search.GovernanceSearchResilienceProperties;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineHealthMetricsRecorder;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineHealthResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineHealthService;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineHealthStatus;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResilienceProperties;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineResilienceMode;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeMetricsRecorder;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeMode;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeSummaryService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class GovernanceConsoleRuntimeSummaryServiceTest {

	@Test
	void shouldReturnNormalWhenConsoleHealthIsHealthy() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeSummaryService service = service(
				true,
				true,
				true,
				false,
				false,
				true,
				true,
				false,
				true,
				true,
				GovernanceTimelineRuntimeMode.NORMAL,
				registry
		);

		GovernanceConsoleRuntimeSummaryResponse response = service.summary().block();

		assertThat(response.runtimeMode()).isEqualTo(GovernanceConsoleRuntimeMode.NORMAL);
		assertThat(response.degradedSignals()).isEmpty();
		assertThat(response.timelineRuntime()).isNotNull();
		assertThat(registry.find(GovernanceConsoleMetricName.RUNTIME_MODE)
				.tag("component", "governance-console-runtime")
				.gauge()
				.value()).isEqualTo(0.0);
	}

	@Test
	void shouldReturnDegradedReadOnlyWhenConsoleHealthIsDegraded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeSummaryService service = service(
				false,
				true,
				true,
				true,
				false,
				true,
				true,
				false,
				true,
				true,
				GovernanceTimelineRuntimeMode.NORMAL,
				registry
		);

		GovernanceConsoleRuntimeSummaryResponse response = service.summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceConsoleRuntimeMode.DEGRADED_READ_ONLY
		);
		assertThat(response.degradedSignals())
				.contains("dashboard:DEGRADED", "console:DEGRADED");
		assertThat(registry.find(GovernanceConsoleMetricName.RUNTIME_MODE)
				.tag("component", "governance-console-runtime")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnAttentionRequiredWhenConsoleHealthRequiresAttention() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeSummaryService service = service(
				true,
				true,
				true,
				false,
				true,
				false,
				true,
				false,
				true,
				true,
				GovernanceTimelineRuntimeMode.NORMAL,
				registry
		);

		GovernanceConsoleRuntimeSummaryResponse response = service.summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED
		);
		assertThat(response.degradedSignals())
				.contains("detail:STRICT", "console:ATTENTION_REQUIRED");
		assertThat(registry.find(GovernanceConsoleMetricName.RUNTIME_MODE)
				.tag("component", "governance-console-runtime")
				.gauge()
				.value()).isEqualTo(2.0);
	}

	@Test
	void shouldIncludeAllDegradedSignals() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeSummaryService service = service(
				false,
				true,
				true,
				true,
				true,
				true,
				true,
				true,
				true,
				true,
				GovernanceTimelineRuntimeMode.NORMAL,
				registry
		);

		GovernanceConsoleRuntimeSummaryResponse response = service.summary().block();

		assertThat(response.degradedSignals())
				.contains("dashboard:DEGRADED", "detail:DEGRADED_CAPABLE", "search:DEGRADED_CAPABLE", "console:DEGRADED");
		assertThat(registry.find(GovernanceConsoleMetricName.RUNTIME_MODE)
				.tag("component", "governance-console-runtime")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldUseTimelineDegradedReadOnlyWhenTimelineRuntimeIsDegraded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeSummaryService service = service(
				true,
				true,
				true,
				false,
				false,
				true,
				true,
				false,
				true,
				true,
				GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY,
				registry
		);

		GovernanceConsoleRuntimeSummaryResponse response = service.summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceConsoleRuntimeMode.DEGRADED_READ_ONLY
		);
		assertThat(response.degradedSignals()).contains(
				"timeline:DEGRADED_READ_ONLY",
				"timeline:DEGRADED_CAPABLE"
		);
	}

	@Test
	void shouldUseTimelineAttentionRequiredWhenTimelineRuntimeRequiresAttention() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeSummaryService service = service(
				true,
				true,
				true,
				false,
				false,
				true,
				true,
				false,
				true,
				true,
				GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED,
				registry
		);

		GovernanceConsoleRuntimeSummaryResponse response = service.summary().block();

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED
		);
		assertThat(response.degradedSignals()).contains(
				"timeline:ATTENTION_REQUIRED",
				"timeline:STRICT"
		);
	}

	private GovernanceConsoleRuntimeSummaryService service(
			boolean dashboardRepositoryAvailable,
			boolean dashboardFallbackEnabled,
			boolean dashboardFailOpen,
			boolean dashboardResilienceEnabled,
			boolean detailEnabled,
			boolean detailPartialEnabled,
			boolean detailFailOpen,
			boolean searchEnabled,
			boolean searchPartialEnabled,
			boolean searchFailOpen,
			GovernanceTimelineRuntimeMode timelineRuntimeMode,
			SimpleMeterRegistry registry
	) {
		GovernanceDashboardHealthService dashboardHealthService =
				dashboardHealthService(
						dashboardRepositoryAvailable,
						dashboardFallbackEnabled,
						dashboardFailOpen,
						dashboardResilienceEnabled
				);
		GovernanceDetailHealthService detailHealthService =
				detailHealthService(
						detailEnabled,
						detailPartialEnabled,
						detailFailOpen,
						1500
				);
		GovernanceSearchHealthService searchHealthService =
				searchHealthService(
						searchEnabled,
						searchPartialEnabled,
						searchFailOpen,
						1500
				);
		GovernanceTimelineRuntimeSummaryService timelineRuntimeSummaryService =
				timelineRuntimeSummaryService(timelineRuntimeMode);

		return new GovernanceConsoleRuntimeSummaryService(
				new GovernanceConsoleHealthService(
						dashboardHealthService,
						detailHealthService,
						searchHealthService,
						new GovernanceConsoleHealthMetricsRecorder(new SimpleMeterRegistry())
				),
				dashboardHealthService,
				detailHealthService,
				searchHealthService,
				timelineRuntimeSummaryService,
				new GovernanceConsoleRuntimeMetricsRecorder(registry)
		);
	}

	private GovernanceDashboardHealthService dashboardHealthService(
			boolean repositoryAvailable,
			boolean fallbackEnabled,
			boolean failOpenDashboard,
			boolean resilienceEnabled
	) {
		GovernanceQueryResilienceProperties properties =
				new GovernanceQueryResilienceProperties();
		properties.setFallbackEnabled(fallbackEnabled);
		properties.setFailOpenDashboard(failOpenDashboard);
		properties.setEnabled(resilienceEnabled);

		return new GovernanceDashboardHealthService(
				repositoryAvailable ? Optional.of(emptyRepository()) : Optional.empty(),
				properties,
				new GovernanceDashboardHealthMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceDetailHealthService detailHealthService(
			boolean enabled,
			boolean partialResponseEnabled,
			boolean failOpenDetail,
			int componentQueryTimeoutMs
	) {
		GovernanceDetailResilienceProperties properties =
				new GovernanceDetailResilienceProperties();
		properties.setEnabled(enabled);
		properties.setPartialResponseEnabled(partialResponseEnabled);
		properties.setFailOpenDetail(failOpenDetail);
		properties.setComponentQueryTimeoutMs(componentQueryTimeoutMs);

		return new GovernanceDetailHealthService(
				properties,
				new GovernanceDetailHealthMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceSearchHealthService searchHealthService(
			boolean enabled,
			boolean partialSearchEnabled,
			boolean failOpenSearch,
			int componentQueryTimeoutMs
	) {
		GovernanceSearchResilienceProperties properties =
				new GovernanceSearchResilienceProperties();
		properties.setEnabled(enabled);
		properties.setPartialSearchEnabled(partialSearchEnabled);
		properties.setFailOpenSearch(failOpenSearch);
		properties.setComponentQueryTimeoutMs(componentQueryTimeoutMs);

		return new GovernanceSearchHealthService(
				properties,
				new GovernanceSearchHealthMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceTimelineRuntimeSummaryService timelineRuntimeSummaryService(
			GovernanceTimelineRuntimeMode runtimeMode
	) {
		GovernanceTimelineHealthStatus healthStatus = switch (runtimeMode) {
			case NORMAL -> GovernanceTimelineHealthStatus.HEALTHY;
			case DEGRADED_READ_ONLY -> GovernanceTimelineHealthStatus.DEGRADED_CAPABLE;
			case ATTENTION_REQUIRED -> GovernanceTimelineHealthStatus.STRICT;
		};
		GovernanceTimelineResilienceMode resilienceMode = runtimeMode
				== GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY
						? GovernanceTimelineResilienceMode.FAIL_OPEN_READ_ONLY
						: GovernanceTimelineResilienceMode.STRICT;
		boolean partial = runtimeMode == GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY;
		boolean failOpen = runtimeMode == GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY;

		GovernanceTimelineHealthService healthService =
				new GovernanceTimelineHealthService(
						new org.springframework.beans.factory.ObjectProvider<>() {
							@Override
							public com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationService getObject(Object... args) {
								return null;
							}

							@Override
							public com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationService getIfAvailable() {
								return null;
							}

							@Override
							public com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationService getIfUnique() {
								return null;
							}

							@Override
							public com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationService getObject() {
								return null;
							}
						},
						new GovernanceTimelineResilienceProperties(),
						new GovernanceTimelineHealthMetricsRecorder(new SimpleMeterRegistry())
				) {
					@Override
					public reactor.core.publisher.Mono<GovernanceTimelineHealthResponse> health() {
						return reactor.core.publisher.Mono.just(
								new GovernanceTimelineHealthResponse(
										java.time.Instant.now(),
										healthStatus,
										resilienceMode,
										partial,
										failOpen,
										true,
										java.util.List.of("component_query_failed"),
										"timeline health"
								)
						);
					}
				};

		return new GovernanceTimelineRuntimeSummaryService(
				healthService,
				new GovernanceTimelineRuntimeMetricsRecorder(new SimpleMeterRegistry())
		);
	}

	private GovernanceDashboardQueryRepository emptyRepository() {
		return new GovernanceDashboardQueryRepository() {
			@Override
			public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
					GovernanceDashboardTimeRange range
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}

			@Override
			public Flux<GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
					GovernanceDashboardTimeRange range,
					GovernanceDashboardBucketSize bucketSize
			) {
				return Flux.empty();
			}
		};
	}
}
