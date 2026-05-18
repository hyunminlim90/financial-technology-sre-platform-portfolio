package com.fintech.sre.agent.governance.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.governance.search.resilience")
public class GovernanceSearchResilienceProperties {

	private boolean enabled = false;
	private boolean partialSearchEnabled = true;
	private boolean failOpenSearch = true;
	private int componentQueryTimeoutMs = 1500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPartialSearchEnabled() {
		return partialSearchEnabled;
	}

	public void setPartialSearchEnabled(boolean partialSearchEnabled) {
		this.partialSearchEnabled = partialSearchEnabled;
	}

	public boolean isFailOpenSearch() {
		return failOpenSearch;
	}

	public void setFailOpenSearch(boolean failOpenSearch) {
		this.failOpenSearch = failOpenSearch;
	}

	public int getComponentQueryTimeoutMs() {
		return componentQueryTimeoutMs;
	}

	public void setComponentQueryTimeoutMs(int componentQueryTimeoutMs) {
		this.componentQueryTimeoutMs = componentQueryTimeoutMs;
	}
}
