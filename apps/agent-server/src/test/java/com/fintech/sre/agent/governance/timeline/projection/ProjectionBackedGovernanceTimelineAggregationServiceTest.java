package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;
import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationRequest;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineAggregationResult;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelinePageMetadata;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelinePageResponse;
import com.fintech.sre.agent.governance.timeline.GovernanceTimelineQuery;

import reactor.core.publisher.Mono;

class ProjectionBackedGovernanceTimelineAggregationServiceTest {

	@Test
	void shouldDelegateToQueryAdapter() {
		CapturingQueryAdapter queryAdapter = new CapturingQueryAdapter(
				GovernanceTimelineAggregationResult.success(
						new GovernanceTimelinePageResponse(
								List.of(
										new GovernanceDetailTimelineItem(
												java.time.Instant.parse("2026-05-17T00:00:00Z"),
												"RECOMMENDATION_CREATED",
												"event-1",
												"INFO",
												"title",
												"summary"
										)
								),
								new GovernanceTimelinePageMetadata(
										null,
										null,
										false,
										false,
										10,
										GovernanceCursorDirection.NEXT,
										"occurredAt DESC, eventId DESC",
										false,
										List.of()
								)
						)
				)
		);
		ProjectionBackedGovernanceTimelineAggregationService service =
				new ProjectionBackedGovernanceTimelineAggregationService(queryAdapter);
		GovernanceTimelineAggregationRequest request = request();

		GovernanceTimelineAggregationResult result = service.aggregate(request).block();

		assertThat(queryAdapter.captured).isSameAs(request);
		assertThat(result).isEqualTo(queryAdapter.result);
	}

	@Test
	void shouldPreserveEmptyPageResult() {
		GovernanceTimelineAggregationResult emptyResult =
				GovernanceTimelineAggregationResult.success(
						new GovernanceTimelinePageResponse(
								List.of(),
								new GovernanceTimelinePageMetadata(
										null,
										null,
										false,
										false,
										50,
										GovernanceCursorDirection.NEXT,
										"occurredAt DESC, eventId DESC",
										false,
										List.of()
								)
						)
				);
		ProjectionBackedGovernanceTimelineAggregationService service =
				new ProjectionBackedGovernanceTimelineAggregationService(
						new CapturingQueryAdapter(emptyResult)
				);

		GovernanceTimelineAggregationResult result =
				service.aggregate(request()).block();

		assertThat(result).isEqualTo(emptyResult);
		assertThat(result.page().items()).isEmpty();
	}

	@Test
	void shouldRejectNullRequest() {
		ProjectionBackedGovernanceTimelineAggregationService service =
				new ProjectionBackedGovernanceTimelineAggregationService(
						new CapturingQueryAdapter(
								GovernanceTimelineAggregationResult.success(
										new GovernanceTimelinePageResponse(
												List.of(),
												new GovernanceTimelinePageMetadata(
														null,
														null,
														false,
														false,
														1,
														GovernanceCursorDirection.NEXT,
														"occurredAt DESC, eventId DESC",
														false,
														List.of()
												)
										)
								)
						)
				);

		assertThatThrownBy(() -> service.aggregate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("request must not be null");
	}

	@Test
	void shouldRejectNullQueryAdapterDependency() {
		assertThatThrownBy(
				() -> new ProjectionBackedGovernanceTimelineAggregationService(null)
		)
				.isInstanceOf(NullPointerException.class)
				.hasMessage("queryAdapter must not be null");
	}

	@Test
	void shouldPropagateAdapterFailure() {
		ProjectionBackedGovernanceTimelineAggregationService service =
				new ProjectionBackedGovernanceTimelineAggregationService(
						request -> Mono.error(new IllegalStateException("adapter failure"))
				);

		assertThatThrownBy(() -> service.aggregate(request()).block())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("adapter failure");
	}

	private GovernanceTimelineAggregationRequest request() {
		return new GovernanceTimelineAggregationRequest(
				new GovernanceTimelineQuery(
						null,
						10,
						GovernanceCursorDirection.NEXT,
						null
				),
				List.of()
		);
	}

	private static final class CapturingQueryAdapter
			implements GovernanceTimelineProjectionQueryAdapter {

		private final GovernanceTimelineAggregationResult result;
		private GovernanceTimelineAggregationRequest captured;

		private CapturingQueryAdapter(GovernanceTimelineAggregationResult result) {
			this.result = result;
		}

		@Override
		public Mono<GovernanceTimelineAggregationResult> query(
				GovernanceTimelineAggregationRequest request
		) {
			this.captured = request;
			return Mono.just(result);
		}
	}
}
