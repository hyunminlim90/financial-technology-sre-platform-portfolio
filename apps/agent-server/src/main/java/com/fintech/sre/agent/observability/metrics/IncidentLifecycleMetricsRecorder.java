package com.fintech.sre.agent.observability.metrics;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;

@Component
public class IncidentLifecycleMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public IncidentLifecycleMetricsRecorder(GovernanceMetricsRecorder recorder) {
		this.recorder = recorder;
	}

	public void recordTransition(IncidentLifecycleRecord record) {
		recorder.increment(
				GovernanceMetricName.INCIDENT_LIFECYCLE_TRANSITION,
				Map.of(
						"from", record.previousStatus() == null ? "NONE" : record.previousStatus().name(),
						"to", record.currentStatus() == null ? "UNKNOWN" : record.currentStatus().name(),
						"reason", record.transitionReason() == null ? "UNKNOWN" : record.transitionReason().name()
				)
		);
	}
}
