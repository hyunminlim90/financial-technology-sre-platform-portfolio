package com.fintech.sre.agent.persistence.r2dbc;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStep;

public final class JsonUtils {

	private static final TypeReference<List<String>> STRING_LIST =
			new TypeReference<>() { };
	private static final TypeReference<Map<String, String>> STRING_MAP =
			new TypeReference<>() { };
	private static final TypeReference<List<ExecutionPlanStep>> EXECUTION_PLAN_STEP_LIST =
			new TypeReference<>() { };

	private JsonUtils() {
	}

	public static String toJsonArray(
			ObjectMapper objectMapper,
			List<String> value,
			String errorMessage
	) {
		try {
			return objectMapper.writeValueAsString(
					value == null ? List.of() : value
			);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	public static String toJsonValue(
			ObjectMapper objectMapper,
			Object value,
			String errorMessage
	) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	public static String toJsonObject(
			ObjectMapper objectMapper,
			Map<String, String> value,
			String errorMessage
	) {
		try {
			return objectMapper.writeValueAsString(
					value == null ? Map.of() : value
			);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	public static List<String> toStringList(
			ObjectMapper objectMapper,
			String value,
			String errorMessage
	) {
		if (value == null || value.isBlank()) {
			return List.of();
		}

		try {
			return objectMapper.readValue(value, STRING_LIST);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	public static Map<String, String> toStringMap(
			ObjectMapper objectMapper,
			String value,
			String errorMessage
	) {
		if (value == null || value.isBlank()) {
			return Map.of();
		}

		try {
			return objectMapper.readValue(value, STRING_MAP);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	public static <T> T toValue(
			ObjectMapper objectMapper,
			String value,
			TypeReference<T> typeReference,
			String errorMessage
	) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return objectMapper.readValue(value, typeReference);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	public static TypeReference<List<ExecutionPlanStep>> executionPlanStepListType() {
		return EXECUTION_PLAN_STEP_LIST;
	}
}
