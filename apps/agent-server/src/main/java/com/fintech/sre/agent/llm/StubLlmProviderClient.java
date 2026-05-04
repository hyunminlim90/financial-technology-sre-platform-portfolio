package com.fintech.sre.agent.llm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class StubLlmProviderClient implements LlmProviderClient {

	private static final Pattern INCIDENT_ID_PATTERN = Pattern.compile("Incident ID: ([^\\n]+)");

	@Override
	public Mono<String> complete(String systemPrompt, String userPrompt) {
		if (userPrompt.contains("structured Incident Recommendation response")) {
			return Mono.just(incidentRecommendationJson(extractIncidentId(userPrompt)));
		}
		return Mono.just("""
				# Postmortem Draft
				## Summary
				Incident analysis draft generated from provided summary and timeline.
				## Root Cause
				Hypothesis: timeout amplification across dependent services.
				## Follow-up
				Validate retry policy, capacity thresholds, and alert tuning.
				""");
	}

	private String extractIncidentId(String userPrompt) {
		Matcher matcher = INCIDENT_ID_PATTERN.matcher(userPrompt);
		return matcher.find() ? matcher.group(1).trim() : "UNKNOWN-INCIDENT";
	}

	private String incidentRecommendationJson(String incidentId) {
		return """
				{
				  "incidentId": "%s",
				  "status": "RECOMMENDATION_CREATED",
				  "incidentSummary": {
				    "failureMode": "LATENCY_AND_TIMEOUT",
				    "domain": "PAYMENTS",
				    "service": "checkout-service",
				    "environment": "prod",
				    "severity": "SEV_2",
				    "impactScope": "PARTIAL"
				  },
				  "mostLikelyCauses": [
				    {
				      "cause": "Downstream timeout amplification",
				      "confidence": "HIGH",
				      "reason": "Metrics, logs, and traces point to retry-driven saturation."
				    }
				  ],
				  "evidence": {
				    "metrics": [
				      {
				        "name": "p95_latency_ms",
				        "value": 920.0,
				        "threshold": 300.0,
				        "status": "abnormal",
				        "query": "histogram_quantile(...)"
				      }
				    ],
				    "logs": [
				      "[ERROR] 2026-05-02T00:00:05Z traceId=trace-001 :: TimeoutException at downstream payment client"
				    ],
				    "traces": [
				      "traceId=trace-001 span=checkout-dependency-call dependency=payment-provider durationMs=1850 status=abnormal"
				    ]
				  },
				  "recommendedActions": [
				    {
				      "step": 1,
				      "action": "Apply controlled rate limiting to protect core payment traffic.",
				      "expectedEffect": "Retry storm pressure should drop and latency should stabilize. Include idempotency and duplicate-payment guard review in follow-up.",
				      "risk": "Can temporarily reduce throughput for non-critical traffic.",
				      "rollbackPlan": "Remove temporary rate limit policy after latency and error rate normalize.",
				      "verification": [
				        "Check p95 latency trend",
				        "Check 5xx error ratio",
				        "Confirm queue depth is falling"
				      ],
				      "requiresHumanApproval": true,
				      "source": "RUNBOOK",
				      "command": {
				        "type": "APPLY_RATE_LIMIT",
				        "targetLayer": "EDGE",
				        "targetService": "checkout-service",
				        "riskLevel": "MEDIUM",
				        "blastRadius": "SERVICE",
				        "preconditions": ["payment_error_rate_high", "traffic_spike_detected"],
				        "forbiddenIf": [],
				        "approvalPolicy": {
				          "required": true
				        },
				        "rollbackPolicy": {
				          "required": true
				        },
				        "verificationPolicy": {
				          "required": true,
				          "checks": [
				            "Check p95 latency trend",
				            "Check 5xx error ratio",
				            "Confirm queue depth is falling"
				          ]
				        },
				        "paymentSafety": {
				          "idempotencySafe": true,
				          "stateTransitionSafe": true,
				          "duplicateExecutionRisk": "LOW"
				        },
				        "humanReadableDescription": "Apply rate limiting to reduce load"
				      }
				    }
				  ],
				  "alternativeActions": [
				    {
				      "action": "Temporarily shed low-priority traffic.",
				      "reasonNotSelected": "Rate limiting was selected first because it is more targeted and easier to rollback."
				    }
				  ],
				  "forbiddenActions": [
				    {
				      "action": "Scale checkout workers conservatively while observing downstream saturation.",
				      "reason": "Improvement constraint blocked scale-out because retry_rate and db_connection_pending are both elevated."
				    }
				  ],
				  "confidenceLevel": "HIGH",
				  "humanApprovalRequired": true,
				  "referencedKnowledge": {
				    "scenarios": ["Checkout Timeout Escalation"],
				    "runbooks": ["API Latency Runbook"],
				    "improvements": ["Retry Storm with DB Pending Constraint"],
				    "preventiveDesigns": ["Idempotency Protection for Payment Paths"],
				    "postmortems": ["Checkout Queue Saturation Postmortem"],
				    "ragDocs": ["Payment Retry Deep Diagnosis"]
				  }
				}
				""".formatted(incidentId);
	}
}
