package com.fintech.sre.agent.governance.timeline;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.governance.detail.GovernanceDetailSanitizer;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

@Component
public class DefaultGovernanceTimelineProjectionMapper
		implements GovernanceTimelineProjectionMapper {

	private static final String AI_ACTOR_ID = "ai";
	private static final String AI_ACTOR_NAME = "AI";
	private static final String SYSTEM_ACTOR_ID = "system";
	private static final String SYSTEM_ACTOR_NAME = "System";
	private static final String HUMAN_ACTOR_NAME = "Human Operator";

	private final GovernanceDetailSanitizer sanitizer;

	public DefaultGovernanceTimelineProjectionMapper(
			GovernanceDetailSanitizer sanitizer
	) {
		this.sanitizer = sanitizer;
	}

	@Override
	public Optional<GovernanceTimelineProjection> project(Object source) {
		if (source == null) {
			return Optional.empty();
		}

		if (source instanceof RecommendationRecord record) {
			return Optional.of(projectRecommendation(record));
		}

		if (source instanceof RecommendationApprovalRecord record) {
			return Optional.of(projectApproval(record));
		}

		if (source instanceof RecommendationExecutionPlan record) {
			return Optional.of(projectExecutionPlan(record));
		}

		if (source instanceof HumanExecutionResultRecord record) {
			return Optional.of(projectHumanExecution(record));
		}

		if (source instanceof VerificationResultRecord record) {
			return Optional.of(projectVerification(record));
		}

		if (source instanceof IncidentLifecycleRecord record) {
			return Optional.of(projectIncidentLifecycle(record));
		}

		if (source instanceof PostmortemDraftRecord record) {
			return Optional.of(projectPostmortemDraft(record));
		}

		if (source instanceof PostmortemReviewRecord record) {
			return Optional.of(projectPostmortemReview(record));
		}

		if (source instanceof LearningCandidateRecord record) {
			return Optional.of(projectLearningCandidate(record));
		}

		if (source instanceof KnowledgePromotionReviewRecord record) {
			return Optional.of(projectKnowledgePromotionReview(record));
		}

		if (source instanceof KnowledgePromotionPlanRecord record) {
			return Optional.of(projectKnowledgePromotionPlan(record));
		}

		if (source instanceof KnowledgeUpdateApplicationRecord record) {
			return Optional.of(projectKnowledgeUpdateApplication(record));
		}

		return Optional.empty();
	}

	private GovernanceTimelineProjection projectRecommendation(
			RecommendationRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.RECOMMENDATION_RECORD;
		String sourceId = safeId(record.recommendationRecordId());
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.RECOMMENDATION_CREATED,
				orEpoch(record.generatedAt()),
				"Recommendation created",
				safeSummary(
						"Recommendation created for service %s with status %s"
								.formatted(
										record.service(),
										sanitizer.safeStatus(record.status())
								)
				),
				GovernanceTimelineSeverity.INFO,
				actor(GovernanceTimelineActorType.AI, AI_ACTOR_ID, AI_ACTOR_NAME),
				resource(
						GovernanceTimelineResourceType.RECOMMENDATION,
						sourceId,
						"Recommendation %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"source", record.source(),
										"service", record.service(),
										"domain", record.domain(),
										"severity", sanitizer.safeStatus(record.severity()),
										"status", sanitizer.safeStatus(record.status()),
										"policyDecision", record.policyDecision(),
										"guardrailDecision", record.guardrailDecision(),
										"recommendedActionCount",
										String.valueOf(record.recommendedActionCount()),
										"forbiddenActionCount",
										String.valueOf(record.forbiddenActionCount()),
										"actionTypes", join(record.actionTypes()),
										"blockedReasons", join(record.blockedReasons())
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectApproval(
			RecommendationApprovalRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.APPROVAL_RECORD;
		String sourceId = safeId(record.approvalId());
		RecommendationApprovalStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.APPROVAL_DECIDED,
				orEpoch(record.decidedAt()),
				"Recommendation approval decided",
				safeSummary(
						"Approval %s for recommendation %s"
								.formatted(
										sanitizer.safeStatus(status),
										safeId(record.recommendationRecordId())
								)
				),
				approvalSeverity(status),
				actor(
						GovernanceTimelineActorType.HUMAN,
						safeId(record.operatorId()),
						HUMAN_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.APPROVAL,
						sourceId,
						"Approval %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"recommendationRecordId",
										safeId(record.recommendationRecordId()),
										"status",
										sanitizer.safeStatus(status),
										"reason",
										record.reason()
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectExecutionPlan(
			RecommendationExecutionPlan record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.EXECUTION_PLAN;
		String sourceId = safeId(record.executionPlanId());
		ExecutionPlanStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.EXECUTION_PLAN_CREATED,
				orEpoch(record.createdAt()),
				"Execution plan created",
				safeSummary(
						"Execution plan %s created with status %s"
								.formatted(sourceId, sanitizer.safeStatus(status))
				),
				executionPlanSeverity(record),
				actor(
						GovernanceTimelineActorType.SYSTEM,
						SYSTEM_ACTOR_ID,
						SYSTEM_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.EXECUTION_PLAN,
						sourceId,
						"Execution plan %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"recommendationRecordId",
										safeId(record.recommendationRecordId()),
										"status",
										sanitizer.safeStatus(record.status()),
										"executable",
										String.valueOf(record.executable()),
										"requiresFinalApproval",
										String.valueOf(record.requiresFinalApproval()),
										"createdBy",
										record.createdBy(),
										"reason",
										record.reason(),
										"stepCount",
										String.valueOf(record.steps().size()),
										"blockedReasons",
										join(record.blockedReasons())
								),
								Map.of()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectVerification(
			VerificationResultRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.VERIFICATION_RESULT;
		String sourceId = safeId(record.verificationResultId());
		VerificationStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.VERIFICATION_RECORDED,
				orEpoch(record.verifiedAt()),
				"Verification recorded",
				safeSummary(record.summary()),
				verificationSeverity(status),
				actor(
						GovernanceTimelineActorType.HUMAN,
						safeId(record.operatorId()),
						HUMAN_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.VERIFICATION,
						sourceId,
						"Verification %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"recommendationRecordId",
										safeId(record.recommendationRecordId()),
										"executionPlanId",
										safeId(record.executionPlanId()),
										"executionResultId",
										safeId(record.executionResultId()),
										"status",
										sanitizer.safeStatus(status)
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectHumanExecution(
			HumanExecutionResultRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.HUMAN_EXECUTION_RESULT;
		String sourceId = safeId(record.executionResultId());
		HumanExecutionStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.HUMAN_EXECUTION_RECORDED,
				orEpoch(record.recordedAt()),
				"Human execution recorded",
				safeSummary(record.summary()),
				humanExecutionSeverity(status),
				actor(
						GovernanceTimelineActorType.HUMAN,
						safeId(record.operatorId()),
						HUMAN_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.HUMAN_EXECUTION,
						sourceId,
						"Human execution %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"recommendationRecordId",
										safeId(record.recommendationRecordId()),
										"executionPlanId",
										safeId(record.executionPlanId()),
										"status",
										sanitizer.safeStatus(status),
										"startedAt",
										stringValue(record.startedAt()),
										"finishedAt",
										stringValue(record.finishedAt())
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectIncidentLifecycle(
			IncidentLifecycleRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.INCIDENT_LIFECYCLE;
		String sourceId = safeId(record.incidentLifecycleId());
		IncidentStatus status = record.currentStatus();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.INCIDENT_TRANSITIONED,
				orEpoch(record.transitionedAt()),
				"Incident transitioned",
				safeSummary(record.summary()),
				incidentSeverity(status),
				actor(
						GovernanceTimelineActorType.SYSTEM,
						SYSTEM_ACTOR_ID,
						SYSTEM_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.INCIDENT,
						safeId(record.incidentId()),
						"Incident %s".formatted(safeId(record.incidentId()))
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"previousStatus",
										sanitizer.safeStatus(record.previousStatus()),
										"currentStatus",
										sanitizer.safeStatus(record.currentStatus()),
										"transitionReason",
										sanitizer.safeStatus(record.transitionReason()),
										"operatorId",
										record.operatorId()
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectPostmortemDraft(
			PostmortemDraftRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.POSTMORTEM_DRAFT;
		String sourceId = safeId(record.postmortemDraftId());
		PostmortemDraftStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.POSTMORTEM_DRAFT_CREATED,
				orEpoch(record.createdAt()),
				"Postmortem draft created",
				safeSummary(record.summary()),
				postmortemDraftSeverity(status),
				actor(GovernanceTimelineActorType.AI, AI_ACTOR_ID, AI_ACTOR_NAME),
				resource(
						GovernanceTimelineResourceType.POSTMORTEM,
						sourceId,
						"Postmortem draft %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"status",
										sanitizer.safeStatus(status),
										"requestedBy",
										record.requestedBy(),
										"timelineCount",
										String.valueOf(record.timeline().size()),
										"recommendationCount",
										String.valueOf(record.recommendations().size()),
										"executionResultCount",
										String.valueOf(record.executionResults().size()),
										"verificationResultCount",
										String.valueOf(record.verificationResults().size()),
										"reanalysisCandidateCount",
										String.valueOf(record.reanalysisCandidates().size()),
										"learningCandidateCount",
										String.valueOf(record.learningCandidates().size()),
										"openQuestions",
										join(record.openQuestions())
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectPostmortemReview(
			PostmortemReviewRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.POSTMORTEM_REVIEW;
		String sourceId = safeId(record.postmortemReviewId());
		PostmortemReviewStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.POSTMORTEM_REVIEWED,
				orEpoch(record.reviewedAt()),
				"Postmortem reviewed",
				safeSummary(record.reviewSummary()),
				postmortemReviewSeverity(status),
				actor(
						GovernanceTimelineActorType.HUMAN,
						safeId(record.reviewedBy()),
						HUMAN_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.POSTMORTEM,
						sourceId,
						"Postmortem review %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"postmortemDraftId",
										safeId(record.postmortemDraftId()),
										"status",
										sanitizer.safeStatus(status),
										"reviewReason",
										record.reviewReason()
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectLearningCandidate(
			LearningCandidateRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.LEARNING_CANDIDATE;
		String sourceId = safeId(record.learningCandidateId());
		LearningCandidateStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.LEARNING_CANDIDATE_CREATED,
				orEpoch(record.createdAt()),
				"Learning candidate created",
				safeSummary(record.summary()),
				learningCandidateSeverity(status),
				actor(
						GovernanceTimelineActorType.SYSTEM,
						SYSTEM_ACTOR_ID,
						SYSTEM_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.LEARNING,
						sourceId,
						"Learning candidate %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"postmortemDraftId",
										safeId(record.postmortemDraftId()),
										"postmortemReviewId",
										safeId(record.postmortemReviewId()),
										"type",
										sanitizer.safeStatus(record.type()),
										"status",
										sanitizer.safeStatus(status),
										"promotedBy",
										record.promotedBy(),
										"proposedChanges",
										join(record.proposedChanges())
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectKnowledgePromotionReview(
			KnowledgePromotionReviewRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.KNOWLEDGE_PROMOTION_REVIEW;
		String sourceId = safeId(record.promotionReviewId());
		KnowledgePromotionReviewStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.PROMOTION_REVIEWED,
				orEpoch(record.reviewedAt()),
				"Knowledge promotion reviewed",
				safeSummary(record.reviewSummary()),
				knowledgePromotionReviewSeverity(status),
				actor(
						GovernanceTimelineActorType.HUMAN,
						safeId(record.reviewedBy()),
						HUMAN_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.KNOWLEDGE_PROMOTION,
						sourceId,
						"Knowledge promotion review %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"learningCandidateId",
										safeId(record.learningCandidateId()),
										"status",
										sanitizer.safeStatus(status),
										"reviewReason",
										record.reviewReason()
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectKnowledgePromotionPlan(
			KnowledgePromotionPlanRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.KNOWLEDGE_PROMOTION_PLAN;
		String sourceId = safeId(record.promotionPlanId());
		KnowledgePromotionPlanStatus status = record.status();
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.PROMOTION_PLAN_CREATED,
				orEpoch(record.createdAt()),
				"Knowledge promotion plan created",
				safeSummary(record.summary()),
				knowledgePromotionPlanSeverity(status),
				actor(
						GovernanceTimelineActorType.SYSTEM,
						SYSTEM_ACTOR_ID,
						SYSTEM_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.KNOWLEDGE_PROMOTION,
						sourceId,
						"Knowledge promotion plan %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"learningCandidateId",
										safeId(record.learningCandidateId()),
										"status",
										sanitizer.safeStatus(status),
										"plannedBy",
										record.plannedBy(),
										"targetCount",
										String.valueOf(record.targets().size()),
										"requiredHumanChecks",
										join(record.requiredHumanChecks()),
										"blockedReasons",
										join(record.blockedReasons())
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineProjection projectKnowledgeUpdateApplication(
			KnowledgeUpdateApplicationRecord record
	) {
		GovernanceTimelineProjectionType sourceType =
				GovernanceTimelineProjectionType.KNOWLEDGE_UPDATE_APPLICATION;
		String sourceId = safeId(record.knowledgeUpdateApplicationId());
		GovernanceTimelineEvent event = new GovernanceTimelineEvent(
				eventId(sourceType, sourceId),
				GovernanceTimelineEventType.KNOWLEDGE_UPDATED,
				orEpoch(record.appliedAt()),
				"Knowledge update applied",
				safeSummary(
						"Knowledge update applied for layer %s with change type %s"
								.formatted(
										sanitizer.safeStatus(record.knowledgeLayer()),
										sanitizer.safeStatus(record.changeType())
								)
				),
				GovernanceTimelineSeverity.INFO,
				actor(
						GovernanceTimelineActorType.HUMAN,
						safeId(record.appliedBy()),
						HUMAN_ACTOR_NAME
				),
				resource(
						GovernanceTimelineResourceType.KNOWLEDGE_UPDATE,
						sourceId,
						"Knowledge update %s".formatted(sourceId)
				),
				new GovernanceTimelineEventMetadata(
						sanitizeAttributes(
								attributeMap(
										"learningCandidateId",
										safeId(record.learningCandidateId()),
										"promotionPlanId",
										safeId(record.promotionPlanId()),
										"knowledgeType",
										record.knowledgeType(),
										"knowledgeLayer",
										sanitizer.safeStatus(record.knowledgeLayer()),
										"changeType",
										sanitizer.safeStatus(record.changeType()),
										"filePath",
										record.filePath(),
										"gitRepository",
										record.gitRepository(),
										"gitBranch",
										record.gitBranch(),
										"pullRequestReference",
										record.pullRequestReference(),
										"reviewedBy",
										record.reviewedBy(),
										"approvedBy",
										record.approvedBy(),
										"validationChecks",
										join(record.validationChecks())
								),
								record.metadata()
						)
				),
				false
		);
		return new GovernanceTimelineProjection(
				sourceType,
				sourceId,
				record.incidentId(),
				event
		);
	}

	private GovernanceTimelineSeverity approvalSeverity(
			RecommendationApprovalStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case APPROVED -> GovernanceTimelineSeverity.INFO;
			case PENDING -> GovernanceTimelineSeverity.WARNING;
			case REJECTED -> GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity executionPlanSeverity(
			RecommendationExecutionPlan record
	) {
		if (record.status() == ExecutionPlanStatus.BLOCKED) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return GovernanceTimelineSeverity.INFO;
	}

	private GovernanceTimelineSeverity verificationSeverity(
			VerificationStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case VERIFIED -> GovernanceTimelineSeverity.INFO;
			case PARTIALLY_VERIFIED -> GovernanceTimelineSeverity.WARNING;
			case NOT_VERIFIED, REGRESSION_DETECTED ->
					GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity humanExecutionSeverity(
			HumanExecutionStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case EXECUTED -> GovernanceTimelineSeverity.INFO;
			case PARTIALLY_EXECUTED, SKIPPED -> GovernanceTimelineSeverity.WARNING;
			case FAILED -> GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity incidentSeverity(IncidentStatus status) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case REOPENED, ESCALATED -> GovernanceTimelineSeverity.WARNING;
			case OPEN, MITIGATING, STABILIZING, RESOLVED ->
					GovernanceTimelineSeverity.INFO;
		};
	}

	private GovernanceTimelineSeverity postmortemDraftSeverity(
			PostmortemDraftStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case DRAFT_CREATED, APPROVED -> GovernanceTimelineSeverity.INFO;
			case HUMAN_REVIEW_REQUIRED -> GovernanceTimelineSeverity.WARNING;
			case REJECTED -> GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity postmortemReviewSeverity(
			PostmortemReviewStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case APPROVED -> GovernanceTimelineSeverity.INFO;
			case PENDING_REVIEW, NEEDS_REVISION -> GovernanceTimelineSeverity.WARNING;
			case REJECTED -> GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity learningCandidateSeverity(
			LearningCandidateStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case PENDING_PROMOTION, APPROVED -> GovernanceTimelineSeverity.INFO;
			case REVIEW_REQUIRED -> GovernanceTimelineSeverity.WARNING;
			case REJECTED -> GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity knowledgePromotionReviewSeverity(
			KnowledgePromotionReviewStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case APPROVED_FOR_PROMOTION -> GovernanceTimelineSeverity.INFO;
			case NEEDS_REVISION -> GovernanceTimelineSeverity.WARNING;
			case REJECTED -> GovernanceTimelineSeverity.ERROR;
		};
	}

	private GovernanceTimelineSeverity knowledgePromotionPlanSeverity(
			KnowledgePromotionPlanStatus status
	) {
		if (status == null) {
			return GovernanceTimelineSeverity.WARNING;
		}
		return switch (status) {
			case PLAN_CREATED -> GovernanceTimelineSeverity.INFO;
			case BLOCKED, NEEDS_HUMAN_EDIT -> GovernanceTimelineSeverity.WARNING;
		};
	}

	private GovernanceTimelineActor actor(
			GovernanceTimelineActorType type,
			String id,
			String displayName
	) {
		return new GovernanceTimelineActor(type, id, displayName);
	}

	private GovernanceTimelineResource resource(
			GovernanceTimelineResourceType type,
			String id,
			String displayName
	) {
		return new GovernanceTimelineResource(type, id, displayName);
	}

	private Map<String, String> sanitizeAttributes(
			Map<String, String> base,
			Map<String, String> additional
	) {
		Map<String, String> sanitized = new LinkedHashMap<>();
		addSanitizedAttributes(sanitized, base);
		addSanitizedAttributes(sanitized, additional);
		return Map.copyOf(sanitized);
	}

	private void addSanitizedAttributes(
			Map<String, String> target,
			Map<String, String> attributes
	) {
		if (attributes == null || attributes.isEmpty()) {
			return;
		}
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			String key = safeAttributeKey(entry.getKey());
			String value = "[redacted]".equals(key)
					? "[redacted]"
					: safeSummary(entry.getValue());
			if (key == null || key.isBlank() || value == null || value.isBlank()) {
				continue;
			}
			target.putIfAbsent(key, value);
		}
	}

	private String safeAttributeKey(String key) {
		String safe = sanitizer.safeText(key);
		return safe == null ? null : safe;
	}

	private String safeSummary(String value) {
		return sanitizer.safeText(value);
	}

	private String eventId(
			GovernanceTimelineProjectionType sourceType,
			String sourceId
	) {
		return sourceType + ":" + sourceId;
	}

	private String safeId(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}

	private Instant orEpoch(Instant value) {
		return value == null ? Instant.EPOCH : value;
	}

	private String join(List<String> values) {
		List<String> safeValues = sanitizer.safeTexts(values);
		return safeValues.isEmpty() ? null : String.join(",", safeValues);
	}

	private String stringValue(Object value) {
		return value == null ? null : value.toString();
	}

	private Map<String, String> attributeMap(String... keyValues) {
		Map<String, String> attributes = new LinkedHashMap<>();
		for (int index = 0; index + 1 < keyValues.length; index += 2) {
			attributes.put(keyValues[index], keyValues[index + 1]);
		}
		return attributes;
	}
}
