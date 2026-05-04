package com.fintech.sre.agent.application;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.service.IncidentActionLogQueryService;
import com.fintech.sre.agent.model.request.PostmortemGenerateByIncidentRequest;
import com.fintech.sre.agent.model.response.PostmortemDraftResponse;
import com.fintech.sre.agent.postmortem.ActionEvaluator;
import com.fintech.sre.agent.postmortem.LearningCandidateExtractor;
import com.fintech.sre.agent.postmortem.PostmortemDraftAssembler;
import com.fintech.sre.agent.postmortem.PostmortemDraftValidator;
import com.fintech.sre.agent.postmortem.PostmortemFilenameRecommender;
import com.fintech.sre.agent.postmortem.PostmortemGenerationInput;
import com.fintech.sre.agent.postmortem.PostmortemIncidentContextBuilder;
import com.fintech.sre.agent.postmortem.RootCauseHypothesisGenerator;
import com.fintech.sre.agent.postmortem.TimelineReconstructor;
import com.fintech.sre.agent.rag.RagRetrievalService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PostmortemGenerationService {

	private final PostmortemRequestValidator requestValidator;
	private final IncidentActionLogQueryService actionLogQueryService;
	private final PostmortemIncidentContextBuilder contextBuilder;
	private final RagRetrievalService ragRetrievalService;
	private final TimelineReconstructor timelineReconstructor;
	private final RootCauseHypothesisGenerator rootCauseHypothesisGenerator;
	private final ActionEvaluator actionEvaluator;
	private final LearningCandidateExtractor learningCandidateExtractor;
	private final PostmortemFilenameRecommender filenameRecommender;
	private final PostmortemDraftAssembler draftAssembler;
	private final PostmortemDraftValidator draftValidator;

	public Mono<PostmortemDraftResponse> generate(PostmortemGenerateByIncidentRequest request) {
		return requestValidator.validate(request)
				.then(actionLogQueryService.findSnapshot(request.incidentId()))
				.flatMap(snapshot -> {
					if (snapshot.recommendations().isEmpty()) {
						return Mono.error(new IllegalStateException(
								"Recommendation history가 없어 Postmortem Draft를 생성할 수 없습니다."
						));
					}
					return Mono.just(snapshot);
				})
				.map(snapshot -> contextBuilder.buildFromSnapshot(request, snapshot))
				.flatMap(context -> ragRetrievalService.retrieve(context.incidentContext())
						.map(rag -> new PostmortemGenerationInput(request, context, rag)))
				.flatMap(this::generateDraft)
				.flatMap(draftValidator::validate);
	}

	private Mono<PostmortemDraftResponse> generateDraft(PostmortemGenerationInput input) {
		return Mono.zip(
				timelineReconstructor.reconstruct(input),
				rootCauseHypothesisGenerator.generate(input),
				actionEvaluator.evaluate(input),
				learningCandidateExtractor.extract(input)
		).map(tuple -> draftAssembler.assemble(
				input,
				tuple.getT1(),
				tuple.getT2(),
				tuple.getT3(),
				tuple.getT4(),
				filenameRecommender.recommend(input)
		));
	}
}
