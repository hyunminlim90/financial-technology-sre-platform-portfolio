package com.fintech.sre.agent.internal.security;

import java.nio.charset.StandardCharsets;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(-100)
public class InternalOperationalApiFilter implements WebFilter {

	private final InternalOperationalApiSecurityProperties properties;

	public InternalOperationalApiFilter(InternalOperationalApiSecurityProperties properties) {
		this.properties = properties;
	}

	@Override
	public Mono<Void> filter(
			ServerWebExchange exchange,
			WebFilterChain chain
	) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().pathWithinApplication().value();

		if (!isProtected(path)) {
			return chain.filter(exchange);
		}

		if (!properties.enabled()) {
			exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
			return exchange.getResponse().setComplete();
		}

		if (!properties.requireHeader()) {
			return chain.filter(exchange);
		}

		if (!properties.hasSecret()) {
			exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
			return write(exchange, "INTERNAL_API_SECRET_NOT_CONFIGURED");
		}

		String headerSecret = request.getHeaders().getFirst(properties.headerNameOrDefault());
		String bearerSecret = bearerToken(request);

		boolean matched =
				constantTimeEquals(headerSecret, properties.headerValue())
						|| constantTimeEquals(bearerSecret, properties.headerValue());

		if (!matched) {
			exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
			return write(exchange, "INVALID_INTERNAL_API_SECRET");
		}

		return chain.filter(exchange);
	}

	private boolean isProtected(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}

		return properties.protectedPathsOrDefault().stream()
				.anyMatch(path::startsWith);
	}

	private String bearerToken(ServerHttpRequest request) {
		String authorization = request.getHeaders().getFirst("Authorization");

		if (authorization == null || authorization.isBlank()) {
			return null;
		}

		return authorization.replaceFirst("(?i)^Bearer\\s+", "");
	}

	private Mono<Void> write(
			ServerWebExchange exchange,
			String message
	) {
		byte[] bytes = ("{\"error\":\"" + message + "\"}")
				.getBytes(StandardCharsets.UTF_8);

		exchange.getResponse().getHeaders()
				.set("Content-Type", "application/json");

		return exchange.getResponse()
				.writeWith(Mono.just(exchange.getResponse()
						.bufferFactory()
						.wrap(bytes)));
	}

	private boolean constantTimeEquals(
			String left,
			String right
	) {
		if (left == null || right == null) {
			return false;
		}

		byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
		byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);

		if (leftBytes.length != rightBytes.length) {
			return false;
		}

		int result = 0;
		for (int i = 0; i < leftBytes.length; i++) {
			result |= leftBytes[i] ^ rightBytes[i];
		}

		return result == 0;
	}
}
