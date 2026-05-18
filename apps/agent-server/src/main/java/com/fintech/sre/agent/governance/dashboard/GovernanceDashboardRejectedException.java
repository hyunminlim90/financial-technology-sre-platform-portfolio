package com.fintech.sre.agent.governance.dashboard;

public class GovernanceDashboardRejectedException extends RuntimeException {

	private final String code;

	public GovernanceDashboardRejectedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
