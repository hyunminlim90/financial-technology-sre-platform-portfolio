package com.fintech.sre.agent.governance.detail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.governance.detail.resilience")
public class GovernanceDetailResilienceProperties {

	private boolean enabled = false;
	private boolean failOpenDetail = true;
	private boolean partialResponseEnabled = true;
	private int componentQueryTimeoutMs = 1500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isFailOpenDetail() {
		return failOpenDetail;
	}

	public void setFailOpenDetail(boolean failOpenDetail) {
		this.failOpenDetail = failOpenDetail;
	}

	public boolean isPartialResponseEnabled() {
		return partialResponseEnabled;
	}

	public void setPartialResponseEnabled(boolean partialResponseEnabled) {
		this.partialResponseEnabled = partialResponseEnabled;
	}

	public int getComponentQueryTimeoutMs() {
		return componentQueryTimeoutMs;
	}

	public void setComponentQueryTimeoutMs(int componentQueryTimeoutMs) {
		this.componentQueryTimeoutMs = componentQueryTimeoutMs;
	}
}
