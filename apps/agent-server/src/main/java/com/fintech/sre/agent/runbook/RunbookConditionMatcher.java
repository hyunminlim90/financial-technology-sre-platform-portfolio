package com.fintech.sre.agent.runbook;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceSignal;

@Component
public class RunbookConditionMatcher {

	public boolean matches(RunbookCondition condition, EvidenceContext evidenceContext) {
		if (condition == null) {
			return true;
		}
		if (!matchesAll(condition.all(), evidenceContext)) {
			return false;
		}
		if (!matchesAny(condition.any(), evidenceContext)) {
			return false;
		}
		return matchesNone(condition.none(), evidenceContext);
	}

	private boolean matchesAll(List<String> signals, EvidenceContext context) {
		if (signals == null || signals.isEmpty()) {
			return true;
		}
		return signals.stream()
				.map(this::toSignal)
				.allMatch(context::hasReliableSignal);
	}

	private boolean matchesAny(List<String> signals, EvidenceContext context) {
		if (signals == null || signals.isEmpty()) {
			return true;
		}
		return signals.stream()
				.map(this::toSignal)
				.anyMatch(context::hasReliableSignal);
	}

	private boolean matchesNone(List<String> signals, EvidenceContext context) {
		if (signals == null || signals.isEmpty()) {
			return true;
		}
		return signals.stream()
				.map(this::toSignal)
				.noneMatch(context::hasReliableSignal);
	}

	private EvidenceSignal toSignal(String value) {
		try {
			return EvidenceSignal.valueOf(value);
		} catch (Exception exception) {
			return EvidenceSignal.UNKNOWN;
		}
	}
}
