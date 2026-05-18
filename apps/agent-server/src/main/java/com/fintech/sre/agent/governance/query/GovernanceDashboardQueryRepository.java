package com.fintech.sre.agent.governance.query;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardBucketSize;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardTimeRange;

import reactor.core.publisher.Flux;

public interface GovernanceDashboardQueryRepository {

	Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
			GovernanceDashboardTimeRange range
	);

	Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
			GovernanceDashboardTimeRange range
	);

	Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
			GovernanceDashboardTimeRange range
	);

	Flux<GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	);

	Flux<GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	);

	Flux<GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	);
}
