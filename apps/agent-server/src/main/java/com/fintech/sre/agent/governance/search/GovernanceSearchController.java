package com.fintech.sre.agent.governance.search;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardErrorResponse;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardRejectedException;

import reactor.core.publisher.Mono;

@RestController
public class GovernanceSearchController {

	private final GovernanceSearchService service;

	public GovernanceSearchController(
			GovernanceSearchService service
	) {
		this.service = service;
	}

	@GetMapping("/internal/governance/search")
	public Mono<GovernanceSearchResponse> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false, defaultValue = "ALL") GovernanceSearchType type,
			@RequestParam(required = false, defaultValue = "24h") String window,
			@RequestParam(required = false) Integer limit
	) {
		return service.search(new GovernanceSearchQuery(q, type, window, limit));
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
