package com.fintech.sre.agent.governance.timeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.governance.timeline.resilience")
public class GovernanceTimelineResilienceProperties {

	private boolean enabled = false;
	private boolean partialTimelineEnabled = true;
	private boolean failOpenReadOnly = true;
	private int componentQueryTimeoutMs = 1500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPartialTimelineEnabled() {
		return partialTimelineEnabled;
	}

	public void setPartialTimelineEnabled(boolean partialTimelineEnabled) {
		this.partialTimelineEnabled = partialTimelineEnabled;
	}

	public boolean isFailOpenReadOnly() {
		return failOpenReadOnly;
	}

	public void setFailOpenReadOnly(boolean failOpenReadOnly) {
		this.failOpenReadOnly = failOpenReadOnly;
	}

	public int getComponentQueryTimeoutMs() {
		return componentQueryTimeoutMs;
	}

	public void setComponentQueryTimeoutMs(int componentQueryTimeoutMs) {
		this.componentQueryTimeoutMs = componentQueryTimeoutMs;
	}
}
