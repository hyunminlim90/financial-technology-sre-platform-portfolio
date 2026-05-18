package com.fintech.sre.agent.governance.detail;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class GovernanceDetailComponentLoader {

	private final GovernanceDetailResilienceProperties properties;

	public GovernanceDetailComponentLoader(
			GovernanceDetailResilienceProperties properties
	) {
		this.properties = properties;
	}

	public <T> Mono<List<T>> list(
			String componentName,
			Mono<List<T>> source,
			List<String> failedComponents,
			AtomicReference<String> degradationReason
	) {
		return protect(
				componentName,
				source,
				List.of(),
				failedComponents,
				degradationReason
		);
	}

	public <T> Mono<Optional<T>> optional(
			String componentName,
			Mono<T> source,
			List<String> failedComponents,
			AtomicReference<String> degradationReason
	) {
		return protect(
				componentName,
				source.map(Optional::of)
						.switchIfEmpty(Mono.just(Optional.empty())),
				Optional.empty(),
				failedComponents,
				degradationReason
		);
	}

	private <T> Mono<T> protect(
			String componentName,
			Mono<T> source,
			T fallback,
			List<String> failedComponents,
			AtomicReference<String> degradationReason
	) {
		if (!properties.isEnabled()) {
			return source;
		}

		return source.timeout(Duration.ofMillis(properties.getComponentQueryTimeoutMs()))
				.onErrorResume(ex -> {
					if (!properties.isPartialResponseEnabled()
							|| !properties.isFailOpenDetail()) {
						return Mono.error(ex);
					}

					addFailedComponent(failedComponents, componentName);
					degradationReason.compareAndSet("none", reasonFor(ex));
					return Mono.just(fallback);
				});
	}

	private void addFailedComponent(
			List<String> failedComponents,
			String componentName
	) {
		synchronized (failedComponents) {
			if (!failedComponents.contains(componentName)) {
				failedComponents.add(componentName);
			}
		}
	}

	private String reasonFor(Throwable ex) {
		return ex instanceof TimeoutException
				? "component_query_timeout"
				: "component_query_failed";
	}
}
