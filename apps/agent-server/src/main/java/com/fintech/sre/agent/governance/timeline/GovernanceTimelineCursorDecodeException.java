package com.fintech.sre.agent.governance.timeline;

public class GovernanceTimelineCursorDecodeException extends RuntimeException {

	public GovernanceTimelineCursorDecodeException() {
		super("Invalid timeline cursor.");
	}
}
