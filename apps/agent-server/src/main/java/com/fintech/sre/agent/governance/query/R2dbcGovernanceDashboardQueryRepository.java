package com.fintech.sre.agent.governance.query;

import org.springframework.context.annotation.Profile;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardBucketSize;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardTimeRange;

import io.r2dbc.spi.Row;
import reactor.core.publisher.Flux;

@Repository
@Profile("r2dbc")
public class R2dbcGovernanceDashboardQueryRepository
		implements GovernanceDashboardQueryRepository {

	private final DatabaseClient databaseClient;

	public R2dbcGovernanceDashboardQueryRepository(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	@Override
	public Flux<GovernanceDashboardQueryResult> findApprovalStatusSummary(
			GovernanceDashboardTimeRange range
	) {
		return databaseClient.sql("""
				SELECT COALESCE(status, 'UNKNOWN') AS name, COUNT(*) AS count
				FROM recommendation_approval_records
				WHERE decided_at >= :from
				  AND decided_at <= :to
				GROUP BY status
				ORDER BY count DESC, name ASC
				""")
				.bind("from", range.from())
				.bind("to", range.to())
				.map(this::mapQueryResult)
				.all();
	}

	@Override
	public Flux<GovernanceDashboardQueryResult> findVerificationStatusSummary(
			GovernanceDashboardTimeRange range
	) {
		return databaseClient.sql("""
				SELECT COALESCE(status, 'UNKNOWN') AS name, COUNT(*) AS count
				FROM verification_result_records
				WHERE verified_at >= :from
				  AND verified_at <= :to
				GROUP BY status
				ORDER BY count DESC, name ASC
				""")
				.bind("from", range.from())
				.bind("to", range.to())
				.map(this::mapQueryResult)
				.all();
	}

	@Override
	public Flux<GovernanceDashboardQueryResult> findLatestIncidentStatusSummary(
			GovernanceDashboardTimeRange range
	) {
		return databaseClient.sql("""
				SELECT latest.current_status AS name, COUNT(*) AS count
				FROM (
					SELECT DISTINCT ON (incident_id)
						incident_id,
						COALESCE(current_status, 'UNKNOWN') AS current_status
					FROM incident_lifecycle_records
					WHERE transitioned_at >= :from
					  AND transitioned_at <= :to
					ORDER BY incident_id, transitioned_at DESC
				) latest
				GROUP BY latest.current_status
				ORDER BY count DESC, name ASC
				""")
				.bind("from", range.from())
				.bind("to", range.to())
				.map(this::mapQueryResult)
				.all();
	}

	@Override
	public Flux<GovernanceDashboardTimeBucketResult> findApprovalStatusBuckets(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	) {
		return bucketQuery(
				"""
						FROM recommendation_approval_records
						WHERE decided_at >= :from
						  AND decided_at < :to
						GROUP BY bucket_start, status
						ORDER BY bucket_start ASC, name ASC
						""",
				"decided_at",
				range,
				bucketSize
		);
	}

	@Override
	public Flux<GovernanceDashboardTimeBucketResult> findVerificationStatusBuckets(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	) {
		return bucketQuery(
				"""
						FROM verification_result_records
						WHERE verified_at >= :from
						  AND verified_at < :to
						GROUP BY bucket_start, status
						ORDER BY bucket_start ASC, name ASC
						""",
				"verified_at",
				range,
				bucketSize
		);
	}

	@Override
	public Flux<GovernanceDashboardTimeBucketResult> findIncidentLifecycleStatusBuckets(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	) {
		String sql = """
				SELECT %s AS bucket_start,
				       COALESCE(current_status, 'UNKNOWN') AS name,
				       COUNT(*) AS count
				FROM incident_lifecycle_records
				WHERE transitioned_at >= :from
				  AND transitioned_at < :to
				GROUP BY bucket_start, current_status
				ORDER BY bucket_start ASC, name ASC
				""".formatted(bucketExpression(bucketSize, "transitioned_at"));

		return databaseClient.sql(sql)
				.bind("from", range.from())
				.bind("to", range.to())
				.map((row, metadata) -> new GovernanceDashboardTimeBucketResult(
						row.get("bucket_start", java.time.OffsetDateTime.class).toInstant(),
						row.get("name", String.class),
						requiredLong(row, "count")
				))
				.all();
	}

	private GovernanceDashboardQueryResult mapQueryResult(Row row, io.r2dbc.spi.RowMetadata metadata) {
		return new GovernanceDashboardQueryResult(
				row.get("name", String.class),
				requiredLong(row, "count")
		);
	}

	private long requiredLong(Row row, String columnName) {
		Long value = row.get(columnName, Long.class);
		return value == null ? 0L : value;
	}

	private Flux<GovernanceDashboardTimeBucketResult> bucketQuery(
			String fromClause,
			String timeColumn,
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize
	) {
		String sql = """
				SELECT %s AS bucket_start,
				       COALESCE(status, 'UNKNOWN') AS name,
				       COUNT(*) AS count
				%s
				""".formatted(bucketExpression(bucketSize, timeColumn), fromClause);

		return databaseClient.sql(sql)
				.bind("from", range.from())
				.bind("to", range.to())
				.map((row, metadata) -> new GovernanceDashboardTimeBucketResult(
						row.get("bucket_start", java.time.OffsetDateTime.class).toInstant(),
						row.get("name", String.class),
						requiredLong(row, "count")
				))
				.all();
	}

	private String bucketExpression(
			GovernanceDashboardBucketSize bucketSize,
			String columnName
	) {
		if (bucketSize == null) {
			return "DATE_TRUNC('hour', " + columnName + ")";
		}

		return switch (bucketSize) {
			case FIFTEEN_MINUTES -> """
					DATE_TRUNC('hour', %s)
					+ make_interval(mins => ((EXTRACT(MINUTE FROM %s)::int / 15) * 15))
					""".formatted(columnName, columnName);
			case ONE_HOUR -> "DATE_TRUNC('hour', " + columnName + ")";
			case ONE_DAY -> "DATE_TRUNC('day', " + columnName + ")";
		};
	}
}
