package com.fintech.sre.agent.postmortem;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.response.PostmortemDraftResponse;

import reactor.core.publisher.Mono;

@Component
public class PostmortemDraftValidator {

	public Mono<PostmortemDraftResponse> validate(PostmortemDraftResponse response) {
		if (!Boolean.TRUE.equals(response.humanValidationRequired())) {
			return Mono.error(new IllegalStateException(
					"Postmortem Draft는 반드시 Human Validation이 필요합니다."
			));
		}

		if (!"draft".equals(response.frontMatter().approvalStatus())) {
			return Mono.error(new IllegalStateException(
					"AI가 생성한 Postmortem은 draft 상태여야 합니다."
			));
		}

		if (response.draft().rootCauseHypotheses() == null
				|| response.draft().rootCauseHypotheses().isEmpty()) {
			return Mono.error(new IllegalStateException(
					"Root Cause 후보가 없는 Postmortem Draft는 허용하지 않습니다."
			));
		}

		return Mono.just(response);
	}
}
