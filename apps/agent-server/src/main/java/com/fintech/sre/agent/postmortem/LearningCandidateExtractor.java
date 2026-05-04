package com.fintech.sre.agent.postmortem;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.response.LearningCandidate;

import reactor.core.publisher.Mono;

@Component
public class LearningCandidateExtractor {

	public Mono<List<LearningCandidate>> extract(PostmortemGenerationInput input) {
		List<LearningCandidate> candidates = new ArrayList<>();
		var snapshot = input.context().actionLogSnapshot();

		boolean failedVerification = snapshot.verifications().stream()
				.anyMatch(verification -> "FAILED".equalsIgnoreCase(verification.status()));
		boolean rollbackExecuted = snapshot.rollbacks() != null && !snapshot.rollbacks().isEmpty();
		boolean wrongScaleOut = snapshot.executedActions().stream()
				.anyMatch(action -> action.action() != null && action.action().toLowerCase().contains("scale-out"))
				&& failedVerification;

		if (wrongScaleOut) {
			candidates.add(new LearningCandidate(
					"improvement",
					"Scale-out restriction after failed verification",
					"scale-out 이후 verification 실패가 기록됨",
					"improvements/scaleout-verification-failure-restriction.md",
					"high"
			));
		}

		if (rollbackExecuted) {
			candidates.add(new LearningCandidate(
					"preventive-design",
					"Rollback-aware incident prevention design",
					"Rollback이 필요했던 장애는 구조적 예방 설계 검토 대상",
					"preventive-designs/rollback-aware-incident-prevention.md",
					"medium"
			));
		}

		return Mono.just(candidates);
	}
}
