package com.fintech.sre.agent.governance.dashboard;

import java.util.List;

public record GovernanceTrendSeries(
		String name,
		List<GovernanceTrendPoint> points
) {
}
