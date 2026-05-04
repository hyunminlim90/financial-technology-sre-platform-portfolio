package com.fintech.sre.agent.incident;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/incidents")
public class IncidentLifecycleController {

	private final IncidentLifecycleService service;

	public IncidentLifecycleController(IncidentLifecycleService service) {
		this.service = service;
	}

	@GetMapping("/{incidentId}/lifecycle")
	public Mono<IncidentLifecycleResponse> findByIncidentId(@PathVariable String incidentId) {
		return service.findByIncidentId(incidentId)
				.map(IncidentLifecycleResponse::from);
	}

	@GetMapping("/lifecycles")
	public Flux<IncidentLifecycleResponse> findAll() {
		return service.findAll()
				.map(IncidentLifecycleResponse::from);
	}

	@PostMapping("/{incidentId}/lifecycle")
	public Mono<IncidentLifecycleResponse> createIfAbsent(@PathVariable String incidentId) {
		return service.createIfAbsent(incidentId)
				.map(IncidentLifecycleResponse::from);
	}

	@PostMapping("/{incidentId}/lifecycle/transition")
	public Mono<IncidentLifecycleResponse> transition(
			@PathVariable String incidentId,
			@RequestBody IncidentTransitionRequest request
	) {
		return service.transition(
						incidentId,
						request.status(),
						request.reason()
				)
				.map(IncidentLifecycleResponse::from);
	}
}
