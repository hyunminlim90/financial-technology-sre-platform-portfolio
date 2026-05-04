package com.fintech.sre.agent.error;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.fintech.sre.agent.exception.ActionLogNotFoundException;
import com.fintech.sre.agent.exception.InsufficientEvidenceException;
import com.fintech.sre.agent.exception.NoScenarioMatchException;
import com.fintech.sre.agent.exception.UnsafeRecommendationException;
import com.fintech.sre.agent.guardrail.GuardrailViolationException;
import com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringException;
import com.fintech.sre.agent.knowledge.rag.KnowledgeConsumerPolicyException;
import com.fintech.sre.agent.policy.PolicyEvaluationException;

import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalErrorHandler {

	@ExceptionHandler(PolicyEvaluationException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handlePolicy(PolicyEvaluationException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Policy evaluation failed",
						ex.violations().stream()
								.map(v -> new ErrorDetail(
										v.code(),
										mapSeverity(v.severity()),
										v.message()
								))
								.toList(),
						"Human must review policy violations before proceeding.",
						Instant.now()
				)));
	}

	@ExceptionHandler(GuardrailViolationException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleGuardrail(GuardrailViolationException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Guardrail validation failed",
						List.of(new ErrorDetail(
								ex.code(),
								ErrorSeverity.BLOCKING,
								ex.getMessage()
						)),
						"Human must review guardrail violation.",
						Instant.now()
				)));
	}

	@ExceptionHandler(KnowledgeLayeringException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleKnowledgeLayering(KnowledgeLayeringException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Knowledge layering validation failed",
						ex.issues().stream()
								.map(issue -> new ErrorDetail(
										issue.code(),
										mapSeverity(issue.severity()),
										issue.message()
								))
								.toList(),
						"Fix knowledge layer mapping before recommending actions.",
						Instant.now()
				)));
	}

	@ExceptionHandler(KnowledgeConsumerPolicyException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleKnowledgeConsumerPolicy(KnowledgeConsumerPolicyException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Knowledge consumer policy validation failed",
						ex.violations().stream()
								.map(v -> new ErrorDetail(
										v.code(),
										ErrorSeverity.BLOCKING,
										v.message()
								))
								.toList(),
						"Scenario and Runbook must exist before Action recommendation.",
						Instant.now()
				)));
	}

	@ExceptionHandler(NoScenarioMatchException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleNoScenarioMatch(NoScenarioMatchException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"No matching scenario found",
						List.of(new ErrorDetail(
								"NO_SCENARIO_MATCH",
								ErrorSeverity.BLOCKING,
								ex.getMessage()
						)),
						"Human operator must review the incident context and scenario mapping.",
						Instant.now()
				)));
	}

	@ExceptionHandler(InsufficientEvidenceException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleInsufficientEvidence(InsufficientEvidenceException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Insufficient evidence",
						List.of(new ErrorDetail(
								"INSUFFICIENT_EVIDENCE",
								ErrorSeverity.ERROR,
								ex.getMessage()
						)),
						"Provide additional observability evidence and retry.",
						Instant.now()
				)));
	}

	@ExceptionHandler(ActionLogNotFoundException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleActionLogNotFound(ActionLogNotFoundException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Action log not found",
						List.of(new ErrorDetail(
								"ACTION_LOG_NOT_FOUND",
								ErrorSeverity.ERROR,
								ex.getMessage()
						)),
						"Verify the requested action log id and retry.",
						Instant.now()
				)));
	}

	@ExceptionHandler(UnsafeRecommendationException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleUnsafeRecommendation(UnsafeRecommendationException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Unsafe recommendation rejected",
						List.of(new ErrorDetail(
								"UNSAFE_RECOMMENDATION",
								ErrorSeverity.BLOCKING,
								ex.getMessage()
						)),
						"Human must review the rejected recommendation.",
						Instant.now()
				)));
	}

	@ExceptionHandler(IllegalStateException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleIllegalState(IllegalStateException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Invalid state transition or invalid application state",
						List.of(new ErrorDetail(
								"INVALID_STATE",
								ErrorSeverity.ERROR,
								ex.getMessage()
						)),
						"Human operator must review the workflow state.",
						Instant.now()
				)));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleIllegalArgument(IllegalArgumentException ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Invalid request",
						List.of(new ErrorDetail(
								"INVALID_REQUEST",
								ErrorSeverity.ERROR,
								ex.getMessage()
						)),
						"Fix request parameters and retry.",
						Instant.now()
				)));
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleValidation(WebExchangeBindException ex) {
		List<ErrorDetail> details = ex.getFieldErrors().stream()
				.map(error -> new ErrorDetail(
						"INVALID_REQUEST",
						ErrorSeverity.ERROR,
						"%s %s".formatted(error.getField(), error.getDefaultMessage())
				))
				.toList();
		return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Invalid request",
						details.isEmpty() ? List.of(new ErrorDetail("INVALID_REQUEST", ErrorSeverity.ERROR, "Request validation failed.")) : details,
						"Fix request parameters and retry.",
						Instant.now()
				)));
	}

	@ExceptionHandler(Exception.class)
	public Mono<ResponseEntity<ApiErrorResponse>> handleUnknown(Exception ex) {
		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						UUID.randomUUID().toString(),
						"Unexpected server error",
						List.of(new ErrorDetail(
								"UNEXPECTED_ERROR",
								ErrorSeverity.ERROR,
								"Unexpected error occurred. Check server logs with errorId."
						)),
						"Human operator must inspect logs. No action was executed.",
						Instant.now()
				)));
	}

	private ErrorSeverity mapSeverity(com.fintech.sre.agent.policy.PolicySeverity severity) {
		return switch (severity) {
			case INFO -> ErrorSeverity.INFO;
			case WARNING -> ErrorSeverity.WARNING;
			case BLOCKING -> ErrorSeverity.BLOCKING;
		};
	}

	private ErrorSeverity mapSeverity(
			com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringIssueSeverity severity
	) {
		return switch (severity) {
			case INFO -> ErrorSeverity.INFO;
			case WARNING -> ErrorSeverity.WARNING;
			case BLOCKING -> ErrorSeverity.BLOCKING;
		};
	}
}
