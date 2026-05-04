package com.fintech.sre.agent.actionlog;

public enum ActionOutcomeStatus {
	NOT_REPORTED,
	MITIGATED,
	PARTIALLY_MITIGATED,
	NOT_EFFECTIVE,
	CAUSED_SIDE_EFFECT,
	ROLLED_BACK
}
