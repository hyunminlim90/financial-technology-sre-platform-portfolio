package com.fintech.sre.agent.governance.query;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.governance.query.resilience")
public class GovernanceQueryResilienceProperties {

	private boolean enabled = false;
	private int optimizedQueryTimeoutMs = 1500;
	private boolean fallbackEnabled = true;
	private boolean failOpenDashboard = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getOptimizedQueryTimeoutMs() {
		return optimizedQueryTimeoutMs;
	}

	public void setOptimizedQueryTimeoutMs(int optimizedQueryTimeoutMs) {
		this.optimizedQueryTimeoutMs = optimizedQueryTimeoutMs;
	}

	public boolean isFallbackEnabled() {
		return fallbackEnabled;
	}

	public void setFallbackEnabled(boolean fallbackEnabled) {
		this.fallbackEnabled = fallbackEnabled;
	}

	public boolean isFailOpenDashboard() {
		return failOpenDashboard;
	}

	public void setFailOpenDashboard(boolean failOpenDashboard) {
		this.failOpenDashboard = failOpenDashboard;
	}
}
