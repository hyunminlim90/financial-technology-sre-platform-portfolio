package com.fintech.sre.agent.action;

public enum ActionType {
	RATE_LIMIT,
	SCALE_OUT,
	SCALE_DOWN,
	RESTART_POD,
	FAILOVER,
	TRAFFIC_SHED,
	OBSERVE_ONLY,
	PAUSE_ROLLOUT
}
