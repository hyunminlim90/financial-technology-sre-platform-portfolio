package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDashboardOverviewService {

	private final GovernanceDashboardService summaryService;
	private final GovernanceDashboardBacklogService backlogService;
	private final GovernanceDashboardTrendService trendService;
	private final GovernanceDashboardRiskService riskService;
	private final GovernanceDashboardMetricsRecorder dashboardMetricsRecorder;

	public GovernanceDashboardOverviewService(
			GovernanceDashboardService summaryService,
			GovernanceDashboardBacklogService backlogService,
			GovernanceDashboardTrendService trendService,
			GovernanceDashboardRiskService riskService,
			GovernanceDashboardMetricsRecorder dashboardMetricsRecorder
	) {
		this.summaryService = summaryService;
		this.backlogService = backlogService;
		this.trendService = trendService;
		this.riskService = riskService;
		this.dashboardMetricsRecorder = dashboardMetricsRecorder;
	}

	public Mono<GovernanceDashboardOverview> overview(
			GovernanceDashboardTrendQuery query
	) {
		GovernanceDashboardTrendQuery safeQuery =
				query == null
						? new GovernanceDashboardTrendQuery("24h", null, null, "1h")
						: query;

		GovernanceDashboardQuery dashboardQuery =
				new GovernanceDashboardQuery(
						safeQuery.window(),
						safeQuery.from(),
						safeQuery.to()
				);

		return Mono.zip(
				summaryService.summary(dashboardQuery),
				backlogService.backlog(dashboardQuery),
				trendService.trends(safeQuery),
				riskService.summary(dashboardQuery)
		).map(tuple -> {
			GovernanceDashboardOverview overview = new GovernanceDashboardOverview(
					Instant.now(),
					tuple.getT1().timeRange(),
					combineDegradation(tuple.getT1().degradation(), tuple.getT3().degradation()),
					tuple.getT1(),
					tuple.getT2(),
					tuple.getT3(),
					tuple.getT4()
			);
			dashboardMetricsRecorder.recordDegradation("overview", overview.degradation());
			return overview;
		});
	}

	private GovernanceDashboardDegradation combineDegradation(
			GovernanceDashboardDegradation summary,
			GovernanceDashboardDegradation trends
	) {
		if (summary != null && summary.degraded()) {
			return summary;
		}

		if (trends != null && trends.degraded()) {
			return trends;
		}

		return GovernanceDashboardDegradation.none();
	}
}
