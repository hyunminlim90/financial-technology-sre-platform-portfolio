package com.fintech.sre.agent.governance.retention;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.governance.retention")
public class GovernanceRetentionProperties {

	private boolean enabled = false;
	private boolean deleteEnabled = false;
	private int hotRetentionDays = 90;
	private int archiveRetentionDays = 365;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isDeleteEnabled() {
		return deleteEnabled;
	}

	public void setDeleteEnabled(boolean deleteEnabled) {
		this.deleteEnabled = deleteEnabled;
	}

	public int getHotRetentionDays() {
		return hotRetentionDays;
	}

	public void setHotRetentionDays(int hotRetentionDays) {
		this.hotRetentionDays = hotRetentionDays;
	}

	public int getArchiveRetentionDays() {
		return archiveRetentionDays;
	}

	public void setArchiveRetentionDays(int archiveRetentionDays) {
		this.archiveRetentionDays = archiveRetentionDays;
	}
}
