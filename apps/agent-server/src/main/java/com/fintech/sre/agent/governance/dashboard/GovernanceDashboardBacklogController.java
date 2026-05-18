package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceDashboardBacklogController {

	private final GovernanceDashboardBacklogService service;

	public GovernanceDashboardBacklogController(
			GovernanceDashboardBacklogService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/dashboard/backlog")
	public Mono<GovernanceDashboardBacklogSummary> backlog(
			@RequestParam(required = false) String window,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			Instant from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			Instant to
	) {
		return service.backlog(new GovernanceDashboardQuery(window, from, to));
	}

	@ExceptionHandler(GovernanceDashboardRejectedException.class)
	public ResponseEntity<GovernanceDashboardErrorResponse> rejected(
			GovernanceDashboardRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new GovernanceDashboardErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
