package com.fintech.sre.agent.decision;

import java.util.Comparator;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.pipeline.DecisionContext;
import com.fintech.sre.agent.decision.pipeline.DecisionPipelineException;
import com.fintech.sre.agent.decision.pipeline.DecisionPipelineStage;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
public class DecisionEngine {

	private final List<DecisionPipelineStage> stages;

	public DecisionEngine(List<DecisionPipelineStage> stages) {
		this.stages = stages.stream()
				.sorted(Comparator.comparingInt(stage -> {
					Order order = stage.getClass().getAnnotation(Order.class);
					return order == null ? Integer.MAX_VALUE : order.value();
				}))
				.toList();
	}

	public Mono<IncidentRecommendationResponse> decide(IncidentRecommendationRequest request) {
		DecisionContext initialContext = DecisionContext.fromRequest(request, null);
		Mono<DecisionContext> pipeline = Mono.just(initialContext);

		for (DecisionPipelineStage stage : stages) {
			pipeline = pipeline.flatMap(context -> executeStage(stage, context));
		}

		return pipeline.map(DecisionContext::response);
	}

	public Mono<IncidentRecommendationResponse> decide(DecisionInput input) {
		DecisionContext initialContext = DecisionContext.fromInput(input);
		Mono<DecisionContext> pipeline = Mono.just(initialContext);

		for (DecisionPipelineStage stage : stages) {
			pipeline = pipeline.flatMap(context -> executeStage(stage, context));
		}

		return pipeline.map(DecisionContext::response);
	}

	private Mono<DecisionContext> executeStage(
			DecisionPipelineStage stage,
			DecisionContext context
	) {
		return stage.execute(context)
				.onErrorResume(ex -> Mono.error(new DecisionPipelineException(
						stage.name(),
						ex
				)));
	}
}
