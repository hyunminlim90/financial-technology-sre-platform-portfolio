package com.fintech.sre.agent.model.request;

import java.time.Instant;

public record DeploymentInfo(
		Boolean recentDeploy,
		Instant deployTime,
		String version,
		String commitSha
) {
}
