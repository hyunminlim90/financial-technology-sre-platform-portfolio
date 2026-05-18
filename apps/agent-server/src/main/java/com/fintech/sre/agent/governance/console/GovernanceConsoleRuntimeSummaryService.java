package com.fintech.sre.agent.governance.console;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthResponse;
import com.fintech.sre.agent.governance.detail.GovernanceDetailHealthResponse;
import com.fintech.sre.agent.governance.search.GovernanceSearchHealthResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeMode;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeSummaryResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineRuntimeSummaryService;

import reactor.core.publisher.Mono;

@Service
public class GovernanceConsoleRuntimeSummaryService {

	private final GovernanceConsoleHealthService consoleHealthService;
	private final com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthService dashboardHealthService;
	private final com.fintech.sre.agent.governance.detail.GovernanceDetailHealthService detailHealthService;
	private final com.fintech.sre.agent.governance.search.GovernanceSearchHealthService searchHealthService;
	private final GovernanceTimelineRuntimeSummaryService timelineRuntimeSummaryService;
	private final GovernanceConsoleRuntimeMetricsRecorder metricsRecorder;

	public GovernanceConsoleRuntimeSummaryService(
			GovernanceConsoleHealthService consoleHealthService,
			com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthService dashboardHealthService,
			com.fintech.sre.agent.governance.detail.GovernanceDetailHealthService detailHealthService,
			com.fintech.sre.agent.governance.search.GovernanceSearchHealthService searchHealthService,
			GovernanceTimelineRuntimeSummaryService timelineRuntimeSummaryService,
			GovernanceConsoleRuntimeMetricsRecorder metricsRecorder
	) {
		this.consoleHealthService = consoleHealthService;
		this.dashboardHealthService = dashboardHealthService;
		this.detailHealthService = detailHealthService;
		this.searchHealthService = searchHealthService;
		this.timelineRuntimeSummaryService = timelineRuntimeSummaryService;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<GovernanceConsoleRuntimeSummaryResponse> summary() {
		return Mono.zip(
				consoleHealthService.health(),
				dashboardHealthService.health(),
				detailHealthService.health(),
				searchHealthService.health(),
				timelineRuntimeSummaryService.summary()
		).map(tuple -> {
			GovernanceConsoleHealthResponse consoleHealth = tuple.getT1();
			GovernanceDashboardHealthResponse dashboardHealth = tuple.getT2();
			GovernanceDetailHealthResponse detailHealth = tuple.getT3();
			GovernanceSearchHealthResponse searchHealth = tuple.getT4();
			GovernanceTimelineRuntimeSummaryResponse timelineRuntime = tuple.getT5();
			GovernanceConsoleRuntimeMode runtimeMode = overrideWithTimeline(
					runtimeMode(consoleHealth.overallStatus()),
					timelineRuntime
			);

			GovernanceConsoleRuntimeSummaryResponse response = new GovernanceConsoleRuntimeSummaryResponse(
					Instant.now(),
					runtimeMode,
					consoleHealth,
					dashboardHealth,
					detailHealth,
					searchHealth,
					timelineRuntime,
					degradedSignals(
							consoleHealth,
							dashboardHealth,
							detailHealth,
							searchHealth,
							timelineRuntime
					),
					message(runtimeMode)
			);

			metricsRecorder.record(response);
			return response;
		});
	}

	private GovernanceConsoleRuntimeMode runtimeMode(
			GovernanceConsoleHealthStatus status
	) {
		return switch (status) {
			case HEALTHY -> GovernanceConsoleRuntimeMode.NORMAL;
			case DEGRADED -> GovernanceConsoleRuntimeMode.DEGRADED_READ_ONLY;
			case ATTENTION_REQUIRED -> GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED;
		};
	}

	private GovernanceConsoleRuntimeMode overrideWithTimeline(
			GovernanceConsoleRuntimeMode current,
			GovernanceTimelineRuntimeSummaryResponse timelineRuntime
	) {
		if (timelineRuntime == null || timelineRuntime.runtimeMode() == null) {
			return current;
		}

		return switch (timelineRuntime.runtimeMode()) {
			case ATTENTION_REQUIRED -> GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED;
			case DEGRADED_READ_ONLY ->
					current == GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED
							? current
							: GovernanceConsoleRuntimeMode.DEGRADED_READ_ONLY;
			case NORMAL -> current;
		};
	}

	private List<String> degradedSignals(
			GovernanceConsoleHealthResponse consoleHealth,
			GovernanceDashboardHealthResponse dashboardHealth,
			GovernanceDetailHealthResponse detailHealth,
			GovernanceSearchHealthResponse searchHealth,
			GovernanceTimelineRuntimeSummaryResponse timelineRuntime
	) {
		List<String> signals = new ArrayList<>();

		if (dashboardHealth != null
				&& dashboardHealth.status() != null
				&& dashboardHealth.status()
						!= com.fintech.sre.agent.governance.dashboard.GovernanceDashboardHealthStatus.HEALTHY) {
			signals.add("dashboard:" + dashboardHealth.status().name());
		}

		if (detailHealth != null
				&& detailHealth.status() != null
				&& detailHealth.status()
						!= com.fintech.sre.agent.governance.detail.GovernanceDetailHealthStatus.HEALTHY) {
			signals.add("detail:" + detailHealth.status().name());
		}

		if (searchHealth != null
				&& searchHealth.status() != null
				&& searchHealth.status()
						!= com.fintech.sre.agent.governance.search.GovernanceSearchHealthStatus.HEALTHY) {
			signals.add("search:" + searchHealth.status().name());
		}

		if (consoleHealth != null
				&& consoleHealth.overallStatus() != null
				&& consoleHealth.overallStatus() != GovernanceConsoleHealthStatus.HEALTHY) {
			signals.add("console:" + consoleHealth.overallStatus().name());
		}

		if (timelineRuntime != null && timelineRuntime.runtimeMode() != null
				&& timelineRuntime.runtimeMode()
						!= GovernanceTimelineRuntimeMode.NORMAL) {
			signals.add("timeline:" + timelineRuntime.runtimeMode().name());
		}

		if (timelineRuntime != null
				&& timelineRuntime.degradedSignals() != null
				&& !timelineRuntime.degradedSignals().isEmpty()) {
			signals.addAll(timelineRuntime.degradedSignals());
		}

		return List.copyOf(signals);
	}

	private String message(GovernanceConsoleRuntimeMode runtimeMode) {
		return switch (runtimeMode) {
			case NORMAL -> "Governance Console runtime is normal.";
			case DEGRADED_READ_ONLY ->
					"Governance Console runtime is degraded but read-only safe.";
			case ATTENTION_REQUIRED ->
					"Governance Console runtime requires operational attention.";
		};
	}
}
