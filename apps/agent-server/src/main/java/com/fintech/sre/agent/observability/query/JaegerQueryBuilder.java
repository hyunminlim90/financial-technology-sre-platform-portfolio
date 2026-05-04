package com.fintech.sre.agent.observability.query;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.observability.model.TraceEvidence;

@Component
public class JaegerQueryBuilder {

	public TraceEvidence toTraceEvidence(String traceId, String serviceName) {
		return new TraceEvidence(
				traceId,
				"checkout-dependency-call",
				serviceName,
				1850L,
				"abnormal",
				"payment-provider",
				"/api/traces/" + traceId
		);
	}
}
