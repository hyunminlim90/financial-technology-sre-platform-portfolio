package com.fintech.sre.agent.incident.lifecycle;

public class IncidentLifecycleRejectedException
		extends RuntimeException {

	private final String code;

	public IncidentLifecycleRejectedException(
			String code,
			String message
	) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
