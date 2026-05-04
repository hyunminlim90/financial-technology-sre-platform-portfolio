package com.fintech.sre.agent.explanation;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.explanation.provider",
		havingValue = "openai"
)
public class LlmExplanationAdapter implements ExplanationPort {

	private final WebClient explanationWebClient;
	private final ExplanationProperties properties;

	public LlmExplanationAdapter(
			WebClient explanationWebClient,
			ExplanationProperties properties
	) {
		this.explanationWebClient = explanationWebClient;
		this.properties = properties;
	}

	@Override
	public Mono<ExplanationResponse> explain(ExplanationRequest request) {
		return explanationWebClient.post()
				.uri("/chat/completions")
				.bodyValue(new ChatCompletionRequest(
						properties.model(),
						List.of(
								new Message("system", systemPrompt()),
								new Message("user", userPrompt(request))
						)
				))
				.retrieve()
				.bodyToMono(ChatCompletionResponse.class)
				.map(response -> new ExplanationResponse(
						request.incidentId(),
						extractContent(response),
						false,
						false,
						true
				));
	}

	private String systemPrompt() {
		return """
				You are an explanation layer for a Human-in-the-loop FinTech SRE AI Agent.

				Rules:
				- Do not decide actions.
				- Do not infer root cause.
				- Do not claim certainty.
				- Do not suggest execution without human approval.
				- Explain only based on DecisionReport.
				- Always state that human review is required.
				- Payment consistency, idempotency, and duplicate payment prevention are mandatory.
				""";
	}

	private String userPrompt(ExplanationRequest request) {
		return """
				Explain the following DecisionReport for a human operator.

				Incident ID:
				%s

				Operator Question:
				%s

				Decision Report Markdown:
				%s

				Required Output:
				- Summary
				- Why actions were recommended
				- Why actions were blocked
				- Evidence used
				- Required human checks
				- Explicitly say root cause is not confirmed
				""".formatted(
				request.incidentId(),
				request.operatorQuestion() == null ? "없음" : request.operatorQuestion(),
				request.decisionReport() == null ? "DecisionReport 없음" : request.decisionReport().markdown()
		);
	}

	private String extractContent(ChatCompletionResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			return "설명 생성 실패: LLM 응답이 비어 있습니다. Human review가 필요합니다.";
		}

		Message message = response.choices().get(0).message();
		return message == null || message.content() == null
				? "설명 생성 실패: 메시지가 비어 있습니다. Human review가 필요합니다."
				: message.content();
	}

	private record ChatCompletionRequest(
			String model,
			List<Message> messages
	) {
	}

	private record ChatCompletionResponse(
			List<Choice> choices
	) {
	}

	private record Choice(
			Message message
	) {
	}

	private record Message(
			String role,
			String content
	) {
	}
}
