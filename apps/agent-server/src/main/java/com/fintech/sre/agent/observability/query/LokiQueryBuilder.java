package com.fintech.sre.agent.observability.query;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.observability.model.QueryEvidence;

@Component
public class LokiQueryBuilder {

	public List<QueryEvidence> build(IncidentAnalyzeRequest request) {
		String service = request.service();

		return List.of(
				new QueryEvidence(
						"error_logs",
						"""
						{service="%s"} |= "ERROR"
						""".formatted(service),
						null,
						"log",
						"error logs"
				),
				new QueryEvidence(
						"timeout_logs",
						"""
						{service="%s"} |= "timeout"
						""".formatted(service),
						null,
						"log",
						"timeout logs"
				),
				new QueryEvidence(
						"redis_logs",
						"""
						{service="%s"} |= "Redis"
						""".formatted(service),
						null,
						"log",
						"Redis related logs"
				)
		);
	}
}
