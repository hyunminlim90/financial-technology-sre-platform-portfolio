package com.fintech.sre.agent.recommendation.approval;

import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditLog;
import com.fintech.sre.agent.recommendation.approval.audit.RecommendationApprovalAuditLogger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class RecommendationApprovalController {

	private final RecommendationApprovalService service;
	private final RecommendationApprovalAuditLogger auditLogger;

	public RecommendationApprovalController(
			RecommendationApprovalService service,
			RecommendationApprovalAuditLogger auditLogger
	) {
		this.service = service;
		this.auditLogger = auditLogger;
	}

	@PostMapping("/internal/recommendations/{id}/approval")
	public Mono<RecommendationApprovalResponse> decide(
			@PathVariable String id,
			@RequestBody RecommendationApprovalRequest request
	) {
		return service.decide(id, request);
	}

	@GetMapping("/internal/recommendations/{id}/approval")
	public Mono<RecommendationApprovalRecord> latest(
			@PathVariable String id
	) {
		return service.findLatest(id);
	}

	@GetMapping("/internal/recommendations/{id}/approval/audit")
	public Flux<RecommendationApprovalAuditLog> auditByRecommendation(
			@PathVariable String id
	) {
		return auditLogger.findByRecommendationRecordId(id);
	}

	@GetMapping("/internal/incidents/{incidentId}/approval/audit")
	public Flux<RecommendationApprovalAuditLog> auditByIncident(
			@PathVariable String incidentId
	) {
		return auditLogger.findByIncidentId(incidentId);
	}

	@ExceptionHandler(RecommendationApprovalRejectedException.class)
	public ResponseEntity<RecommendationApprovalErrorResponse> rejected(
			RecommendationApprovalRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new RecommendationApprovalErrorResponse(
				ex.code(),
				ex.getMessage()
		));
	}
}
