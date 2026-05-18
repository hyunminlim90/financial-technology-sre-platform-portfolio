package com.fintech.sre.agent.incident.lifecycle;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController("internalIncidentLifecycleController")
public class IncidentLifecycleController {

	private final IncidentLifecycleService service;
	private final IncidentLifecycleStore store;

	public IncidentLifecycleController(
			IncidentLifecycleService service,
			IncidentLifecycleStore store
	) {
		this.service = service;
		this.store = store;
	}

	@PostMapping("/internal/incidents/{id}/lifecycle/transition")
	public Mono<IncidentLifecycleTransitionResponse> transition(
			@PathVariable String id,
			@RequestBody IncidentLifecycleTransitionRequest request
	) {
		return service.transition(id, request);
	}

	@GetMapping("/internal/incidents/{id}/lifecycle/latest")
	public Mono<IncidentLifecycleRecord> latest(
			@PathVariable String id
	) {
		return service.latest(id);
	}

	@GetMapping("/internal/incidents/{id}/lifecycle/history")
	public Flux<IncidentLifecycleRecord> history(
			@PathVariable String id
	) {
		return store.findByIncidentId(id);
	}

	@ExceptionHandler(IncidentLifecycleRejectedException.class)
	public ResponseEntity<IncidentLifecycleErrorResponse> rejected(
			IncidentLifecycleRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new IncidentLifecycleErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
