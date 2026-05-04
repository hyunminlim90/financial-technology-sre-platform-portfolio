package com.fintech.sre.agent.decision.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/decision-reports")
public class DecisionReportController {

	private final DecisionReportService service;

	public DecisionReportController(DecisionReportService service) {
		this.service = service;
	}

	@GetMapping("/{reportId}")
	public Mono<DecisionReportResponse> findById(@PathVariable String reportId) {
		return service.findById(reportId)
				.map(DecisionReportResponse::from);
	}

	@GetMapping("/incidents/{incidentId}")
	public Flux<DecisionReportResponse> findByIncidentId(@PathVariable String incidentId) {
		return service.findByIncidentId(incidentId)
				.map(DecisionReportResponse::from);
	}
}
