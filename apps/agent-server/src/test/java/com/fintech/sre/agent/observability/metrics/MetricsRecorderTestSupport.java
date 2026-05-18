package com.fintech.sre.agent.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public final class MetricsRecorderTestSupport {

	private MetricsRecorderTestSupport() {
	}

	public static ApprovalMetricsRecorder approvalMetricsRecorder() {
		return new ApprovalMetricsRecorder(governanceMetricsRecorder());
	}

	public static ExecutionMetricsRecorder executionMetricsRecorder() {
		return new ExecutionMetricsRecorder(governanceMetricsRecorder());
	}

	public static IncidentLifecycleMetricsRecorder incidentLifecycleMetricsRecorder() {
		return new IncidentLifecycleMetricsRecorder(governanceMetricsRecorder());
	}

	public static LearningMetricsRecorder learningMetricsRecorder() {
		return new LearningMetricsRecorder(governanceMetricsRecorder());
	}

	public static VerificationMetricsRecorder verificationMetricsRecorder() {
		return new VerificationMetricsRecorder(governanceMetricsRecorder());
	}

	private static GovernanceMetricsRecorder governanceMetricsRecorder() {
		return new GovernanceMetricsRecorder(new SimpleMeterRegistry());
	}
}
