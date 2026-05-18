package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceTimelineController {

	private static final String INVALID_CURSOR_CODE = "INVALID_TIMELINE_CURSOR";
	private static final String INVALID_QUERY_CODE = "INVALID_TIMELINE_QUERY";
	private static final String QUERY_FAILURE_CODE = "TIMELINE_QUERY_FAILED";
	private static final String QUERY_FAILURE_MESSAGE = "Timeline query failed.";

	private final GovernanceTimelineAggregationService aggregationService;
	private final GovernanceTimelineQueryParser queryParser;
	private final GovernanceTimelineMetricsRecorder metricsRecorder;

	public GovernanceTimelineController(
			GovernanceTimelineAggregationService aggregationService,
			GovernanceTimelineQueryParser queryParser,
			GovernanceTimelineMetricsRecorder metricsRecorder
	) {
		this.aggregationService = aggregationService;
		this.queryParser = queryParser;
		this.metricsRecorder = metricsRecorder;
	}

	@GetMapping("/internal/governance/timeline")
	public Mono<GovernanceTimelineApiResponse> timeline(
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) String direction,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(required = false, name = "eventType") List<String> eventTypes,
			@RequestParam(required = false) Boolean includeDegraded
	) {
		GovernanceTimelineAggregationRequest request = queryParser.parse(
				cursor,
				direction,
				limit,
				from,
				to,
				eventTypes,
				includeDegraded
		);

		return aggregationService.aggregate(request)
				.map(result -> response(result, request.query().safeDirection()))
				.doOnError(GovernanceTimelineCursorDecodeException.class, ex ->
						metricsRecorder.query(
								"invalid_cursor",
								request.query().safeDirection()
						))
				.doOnError(ex -> {
					if (!(ex instanceof GovernanceTimelineCursorDecodeException)) {
						metricsRecorder.query(
								"failure",
								request.query().safeDirection()
						);
					}
				});
	}

	@ExceptionHandler(GovernanceTimelineCursorDecodeException.class)
	public ResponseEntity<GovernanceTimelineApiResponse> invalidCursor(
			GovernanceTimelineCursorDecodeException ex
	) {
		return ResponseEntity.badRequest().body(errorResponse(
				INVALID_CURSOR_CODE,
				ex.getMessage()
		));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<GovernanceTimelineApiResponse> invalidQuery(
			IllegalArgumentException ex
	) {
		metricsRecorder.query("invalid_query", null);
		return ResponseEntity.badRequest().body(errorResponse(
				INVALID_QUERY_CODE,
				ex.getMessage()
		));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<GovernanceTimelineApiResponse> failure(
			Exception ex
	) {
		return ResponseEntity.internalServerError().body(errorResponse(
				QUERY_FAILURE_CODE,
				QUERY_FAILURE_MESSAGE
		));
	}

	private GovernanceTimelineApiResponse response(
			GovernanceTimelineAggregationResult result,
			GovernanceCursorDirection direction
	) {
		boolean degraded = result != null && result.degraded();
		boolean empty = result == null
				|| result.page() == null
				|| result.page().items() == null
				|| result.page().items().isEmpty();
		metricsRecorder.query(
				degraded ? "degraded" : empty ? "empty" : "success",
				direction
		);
		return new GovernanceTimelineApiResponse(
				Instant.now(),
				degraded
						? GovernanceTimelineApiStatus.DEGRADED
						: GovernanceTimelineApiStatus.SUCCESS,
				result == null ? null : result.page(),
				degraded ? degradation(result) : GovernanceTimelineDegradation.none(),
				List.of()
		);
	}

	private GovernanceTimelineDegradation degradation(
			GovernanceTimelineAggregationResult result
	) {
		return GovernanceTimelineDegradation.partial(
				GovernanceTimelineResilienceMode.PARTIAL_DEGRADED,
				result.failedSources().stream()
						.map(source -> new GovernanceTimelineComponentFailure(
								GovernanceTimelineAggregationSource.valueOf(source),
								result.reason()
						))
						.toList(),
				result.reason()
		);
	}

	private GovernanceTimelineApiResponse errorResponse(
			String code,
			String message
	) {
		return new GovernanceTimelineApiResponse(
				Instant.now(),
				GovernanceTimelineApiStatus.FAILURE,
				null,
				GovernanceTimelineDegradation.none(),
				List.of(new GovernanceTimelineApiError(code, message))
		);
	}
}
