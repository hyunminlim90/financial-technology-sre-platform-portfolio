package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.runtime.action.ActionCommand;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationReason;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationResult;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationScope;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationStatus;
import com.fintech.sre.agent.runtime.action.ActionCommandLevel;
import com.fintech.sre.agent.runtime.action.ActionCommandReason;
import com.fintech.sre.agent.runtime.action.ActionCommandScope;
import com.fintech.sre.agent.runtime.approval.ApprovalDecision;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationReason;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationScope;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionIntegrationStatus;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionLevel;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionReason;
import com.fintech.sre.agent.runtime.approval.ApprovalDecisionScope;
import com.fintech.sre.agent.runtime.approval.ApprovalRequest;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationReason;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationScope;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestIntegrationStatus;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestLevel;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestReason;
import com.fintech.sre.agent.runtime.approval.ApprovalRequestScope;
import com.fintech.sre.agent.runtime.approval.ApprovalState;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationReason;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationResult;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationScope;
import com.fintech.sre.agent.runtime.approval.ApprovalStateIntegrationStatus;
import com.fintech.sre.agent.runtime.approval.ApprovalStateLevel;
import com.fintech.sre.agent.runtime.approval.ApprovalStateReason;
import com.fintech.sre.agent.runtime.approval.ApprovalStateScope;
import com.fintech.sre.agent.runtime.recommendation.RecommendationModelReason;
import com.fintech.sre.agent.runtime.recommendation.RecommendationModelType;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentation;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationReason;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationResult;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationScope;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationIntegrationStatus;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationReason;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationScope;
import com.fintech.sre.agent.runtime.recommendation.RecommendationPresentationStatus;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.verification.VerificationRequest;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationReason;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationResult;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationScope;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationStatus;
import com.fintech.sre.agent.runtime.verification.VerificationRequestLevel;
import com.fintech.sre.agent.runtime.verification.VerificationRequestReason;
import com.fintech.sre.agent.runtime.verification.VerificationRequestScope;

class OperationalReliabilityExecutionEngineSelectorIntegrationTest {

	private static final String APPROVAL_STATE_IDENTIFIER = "approval-state/payments/001";
	private static final String DECISION_IDENTIFIER = "approval-decision/payments/001";
	private static final String OPERATOR_CONTEXT = "operator/oncall/payments";
	private static final String APPROVAL_POLICY = "policy/high-risk-human-approval";
	private static final String VERIFICATION_REQUEST_IDENTIFIER = "verification-request/payments/001";
	private static final String VERIFICATION_POLICY = "policy/post-change-verification";
	private static final String ACTION_COMMAND_IDENTIFIER = "action-command/payments/001";
	private static final String ACTION_TYPE = "ROLLING_RESTART";
	private static final String TARGET_LAYER = "KUBERNETES_WORKLOAD";
	private static final String BLAST_RADIUS = "namespace/payments-prod";
	private static final String EXECUTION_PERMISSION_IDENTIFIER = "execution-permission/payments/001";
	private static final String OPERATOR_AUTHORIZATION = "authorized/oncall/payments";
	private static final String EXECUTION_PLAN_IDENTIFIER = "execution-plan/payments/001";
	private static final String EXECUTION_SEQUENCE = "step-1: cordon; step-2: rolling restart";
	private static final String DISPATCH_IDENTIFIER = "dispatch/payments/001";
	private static final String EXECUTION_ENDPOINT = "engine://payments-prod/restart";
	private static final String DISPATCH_POLICY = "policy/manual-dispatch";
	private static final String EXECUTION_ENGINE_IDENTIFIER = "execution-engine/payments/001";
	private static final String EXECUTION_ENGINE_TYPE = "KUBERNETES_MANUAL_GATE";
	private static final String EXECUTION_POLICY = "policy/execution-engine-selection";
	private static final String REGISTRY_IDENTIFIER = "execution-engine-registry/payments/001";
	private static final String REGISTRY_POLICY = "policy/execution-engine-registry";
	private static final String SELECTOR_IDENTIFIER = "execution-engine-selector/payments/001";
	private static final String ENGINE_SELECTION_POLICY = "policy/manual-engine-selection";
	private static final String ENGINE_CAPABILITY_REQUIREMENT = "capability:restart-workload";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-20T00:00:00Z");

	private final ExecutionEngineSelectorIntegration integration =
			new ExecutionEngineSelectorIntegration();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				selectorWithLevel(ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.actualEngineSelection()).isFalse();
		assertThat(result.registryLookup()).isFalse();
		assertThat(result.engineDiscovery()).isFalse();
		assertThat(result.springBeanLookup()).isFalse();
		assertThat(result.serviceLoaderLookup()).isFalse();
		assertThat(result.actionExecution()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBeExecutionEngineSelectorReadyViewWhenSelectorIsReady() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				selectorWithLevel(ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.EXECUTION_ENGINE_SELECTOR_READY_VIEW
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.EXECUTION_ENGINE_SELECTOR_READY
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW
		);
		assertThat(result.operatorFacingExecutionEngineSelectorVisible()).isTrue();
		assertThat(result.executionEngineSelectorCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRemainPartialWhenSelectorIsPartial() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				selectorWithLevel(ExecutionEngineSelectorLevel.PARTIAL)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.PARTIAL_EXECUTION_ENGINE_SELECTOR
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.PARTIAL_EXECUTION_ENGINE_SELECTOR
		);
	}

	@Test
	void shouldRemainNotReadyWhenSelectorIsNotReady() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				selectorWithLevel(ExecutionEngineSelectorLevel.NOT_READY)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.NOT_READY
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.NOT_READY_EXECUTION_ENGINE_SELECTOR
		);
	}

	@Test
	void shouldRemainUnreliableWhenSelectorIsUnreliable() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				selectorWithLevel(ExecutionEngineSelectorLevel.UNRELIABLE)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.UNRELIABLE
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.UNRELIABLE_EXECUTION_ENGINE_SELECTOR
		);
	}

	@Test
	void shouldRemainBlockedWhenSelectorIsBlocked() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				selectorWithLevel(ExecutionEngineSelectorLevel.BLOCKED)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.BLOCKED_EXECUTION_ENGINE_SELECTOR
		);
	}

	@Test
	void shouldBlockWhenSelectorIdentifierMissing() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				new ExecutionEngineSelector(
						ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
						registryReadyView(),
						" ",
						ENGINE_SELECTION_POLICY,
						ENGINE_CAPABILITY_REQUIREMENT,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.MISSING_SELECTOR_IDENTIFIER
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.EXECUTION_ENGINE_SELECTOR
		);
	}

	@Test
	void shouldBlockWhenEngineSelectionPolicyMissing() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				new ExecutionEngineSelector(
						ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
						registryReadyView(),
						SELECTOR_IDENTIFIER,
						" ",
						ENGINE_CAPABILITY_REQUIREMENT,
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.MISSING_ENGINE_SELECTION_POLICY
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.ENGINE_SELECTION_POLICY
		);
	}

	@Test
	void shouldBlockWhenEngineCapabilityRequirementMissing() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				new ExecutionEngineSelector(
						ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
						registryReadyView(),
						SELECTOR_IDENTIFIER,
						ENGINE_SELECTION_POLICY,
						" ",
						true,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.MISSING_ENGINE_CAPABILITY_REQUIREMENT
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.ENGINE_CAPABILITY_REQUIREMENT
		);
	}

	@Test
	void shouldBlockWhenSelectorGuardrailMissing() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				new ExecutionEngineSelector(
						ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
						registryReadyView(),
						SELECTOR_IDENTIFIER,
						ENGINE_SELECTION_POLICY,
						ENGINE_CAPABILITY_REQUIREMENT,
						false,
						OperationalUncertainty.LOW,
						false
				)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.MISSING_SELECTOR_GUARDRAIL
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.SELECTOR_GUARDRAIL
		);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				new ExecutionEngineSelector(
						ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
						registryReadyView(),
						SELECTOR_IDENTIFIER,
						ENGINE_SELECTION_POLICY,
						ENGINE_CAPABILITY_REQUIREMENT,
						true,
						OperationalUncertainty.LOW,
						true
				)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.PAYMENT_SAFETY
		);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionEngineSelectorIntegrationResult result = integration.integrate(
				new ExecutionEngineSelector(
						ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
						registryReadyView(),
						SELECTOR_IDENTIFIER,
						ENGINE_SELECTION_POLICY,
						ENGINE_CAPABILITY_REQUIREMENT,
						true,
						OperationalUncertainty.CRITICAL,
						false
				)
		);

		assertThat(result.status()).isEqualTo(
				ExecutionEngineSelectorIntegrationStatus.BLOCKED
		);
		assertThat(result.reason()).isEqualTo(
				ExecutionEngineSelectorIntegrationReason.CRITICAL_LIFECYCLE_RISK
		);
		assertThat(result.scope()).isEqualTo(
				ExecutionEngineSelectorIntegrationScope.LIFECYCLE_RISK
		);
	}

	@Test
	void shouldRejectNullExecutionEngineSelector() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("executionEngineSelector must not be null");
	}

	private ExecutionEngineSelector selectorWithLevel(
			ExecutionEngineSelectorLevel level
	) {
		return new ExecutionEngineSelector(
				level,
				selectorReason(level),
				ExecutionEngineSelectorScope.EXECUTION_ENGINE_SELECTOR,
				registryReadyView(),
				SELECTOR_IDENTIFIER,
				ENGINE_SELECTION_POLICY,
				ENGINE_CAPABILITY_REQUIREMENT,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionEngineSelectorReason selectorReason(
			ExecutionEngineSelectorLevel level
	) {
		return switch (level) {
			case EXECUTION_ENGINE_SELECTOR_READY ->
				ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY;
			case PARTIAL ->
				ExecutionEngineSelectorReason.PARTIAL_EXECUTION_ENGINE_REGISTRY;
			case NOT_READY ->
				ExecutionEngineSelectorReason.NOT_READY_EXECUTION_ENGINE_REGISTRY;
			case UNRELIABLE ->
				ExecutionEngineSelectorReason.UNRELIABLE_EXECUTION_ENGINE_REGISTRY;
			case BLOCKED ->
				ExecutionEngineSelectorReason.BLOCKED_EXECUTION_ENGINE_REGISTRY;
			case UNKNOWN -> ExecutionEngineSelectorReason.UNKNOWN;
		};
	}

	private ExecutionEngineRegistryIntegrationResult registryReadyView() {
		return registryWithStatus(
				ExecutionEngineRegistryIntegrationStatus.EXECUTION_ENGINE_REGISTRY_READY_VIEW
		);
	}

	private ExecutionEngineRegistryIntegrationResult registryWithStatus(
			ExecutionEngineRegistryIntegrationStatus status
	) {
		return new ExecutionEngineRegistryIntegrationResult(
				registry(status),
				status,
				registryIntegrationReason(status),
				ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
				status == ExecutionEngineRegistryIntegrationStatus.EXECUTION_ENGINE_REGISTRY_READY_VIEW,
				status == ExecutionEngineRegistryIntegrationStatus.EXECUTION_ENGINE_REGISTRY_READY_VIEW
		);
	}

	private ExecutionEngineRegistry registry(
			ExecutionEngineRegistryIntegrationStatus status
	) {
		return new ExecutionEngineRegistry(
				registryLevel(status),
				registryReason(status),
				ExecutionEngineRegistryScope.EXECUTION_ENGINE_REGISTRY,
				executionEngineReadyView(),
				REGISTRY_IDENTIFIER,
				true,
				REGISTRY_POLICY,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionEngineRegistryLevel registryLevel(
			ExecutionEngineRegistryIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_REGISTRY_READY_VIEW ->
				ExecutionEngineRegistryLevel.EXECUTION_ENGINE_REGISTRY_READY;
			case PARTIAL_EXECUTION_ENGINE_REGISTRY ->
				ExecutionEngineRegistryLevel.PARTIAL;
			case NOT_READY -> ExecutionEngineRegistryLevel.NOT_READY;
			case UNRELIABLE -> ExecutionEngineRegistryLevel.UNRELIABLE;
			case BLOCKED -> ExecutionEngineRegistryLevel.BLOCKED;
			case UNKNOWN -> ExecutionEngineRegistryLevel.UNKNOWN;
		};
	}

	private ExecutionEngineRegistryReason registryReason(
			ExecutionEngineRegistryIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_REGISTRY_READY_VIEW ->
				ExecutionEngineRegistryReason.EXECUTION_ENGINE_READY;
			case PARTIAL_EXECUTION_ENGINE_REGISTRY ->
				ExecutionEngineRegistryReason.PARTIAL_EXECUTION_ENGINE;
			case NOT_READY ->
				ExecutionEngineRegistryReason.NOT_READY_EXECUTION_ENGINE;
			case UNRELIABLE ->
				ExecutionEngineRegistryReason.UNRELIABLE_EXECUTION_ENGINE;
			case BLOCKED ->
				ExecutionEngineRegistryReason.BLOCKED_EXECUTION_ENGINE;
			case UNKNOWN -> ExecutionEngineRegistryReason.UNKNOWN;
		};
	}

	private ExecutionEngineRegistryIntegrationReason registryIntegrationReason(
			ExecutionEngineRegistryIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_REGISTRY_READY_VIEW ->
				ExecutionEngineRegistryIntegrationReason.EXECUTION_ENGINE_REGISTRY_READY;
			case PARTIAL_EXECUTION_ENGINE_REGISTRY ->
				ExecutionEngineRegistryIntegrationReason.PARTIAL_EXECUTION_ENGINE_REGISTRY;
			case NOT_READY ->
				ExecutionEngineRegistryIntegrationReason.NOT_READY_EXECUTION_ENGINE_REGISTRY;
			case UNRELIABLE ->
				ExecutionEngineRegistryIntegrationReason.UNRELIABLE_EXECUTION_ENGINE_REGISTRY;
			case BLOCKED ->
				ExecutionEngineRegistryIntegrationReason.BLOCKED_EXECUTION_ENGINE_REGISTRY;
			case UNKNOWN -> ExecutionEngineRegistryIntegrationReason.UNKNOWN;
		};
	}

	private ExecutionEngineIntegrationResult executionEngineReadyView() {
		return executionEngineWithStatus(
				ExecutionEngineIntegrationStatus.EXECUTION_ENGINE_READY_VIEW
		);
	}

	private ExecutionEngineIntegrationResult executionEngineWithStatus(
			ExecutionEngineIntegrationStatus status
	) {
		return new ExecutionEngineIntegrationResult(
				executionEngine(status),
				status,
				executionEngineIntegrationReason(status),
				ExecutionEngineIntegrationScope.OPERATOR_VIEW,
				status == ExecutionEngineIntegrationStatus.EXECUTION_ENGINE_READY_VIEW,
				status == ExecutionEngineIntegrationStatus.EXECUTION_ENGINE_READY_VIEW
		);
	}

	private ExecutionEngine executionEngine(ExecutionEngineIntegrationStatus status) {
		return new ExecutionEngine(
				executionEngineLevel(status),
				executionEngineReason(status),
				ExecutionEngineScope.EXECUTION_ENGINE,
				dispatchReadyView(),
				EXECUTION_ENGINE_IDENTIFIER,
				EXECUTION_ENGINE_TYPE,
				EXECUTION_ENDPOINT,
				EXECUTION_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionEngineLevel executionEngineLevel(
			ExecutionEngineIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_READY_VIEW -> ExecutionEngineLevel.EXECUTION_ENGINE_READY;
			case PARTIAL_EXECUTION_ENGINE -> ExecutionEngineLevel.PARTIAL;
			case NOT_READY -> ExecutionEngineLevel.NOT_READY;
			case UNRELIABLE -> ExecutionEngineLevel.UNRELIABLE;
			case BLOCKED -> ExecutionEngineLevel.BLOCKED;
			case UNKNOWN -> ExecutionEngineLevel.UNKNOWN;
		};
	}

	private ExecutionEngineReason executionEngineReason(
			ExecutionEngineIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_READY_VIEW -> ExecutionEngineReason.DISPATCH_READY;
			case PARTIAL_EXECUTION_ENGINE -> ExecutionEngineReason.PARTIAL_DISPATCH;
			case NOT_READY -> ExecutionEngineReason.NOT_READY_DISPATCH;
			case UNRELIABLE -> ExecutionEngineReason.UNRELIABLE_DISPATCH;
			case BLOCKED -> ExecutionEngineReason.BLOCKED_DISPATCH;
			case UNKNOWN -> ExecutionEngineReason.UNKNOWN;
		};
	}

	private ExecutionEngineIntegrationReason executionEngineIntegrationReason(
			ExecutionEngineIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_READY_VIEW ->
				ExecutionEngineIntegrationReason.EXECUTION_ENGINE_READY;
			case PARTIAL_EXECUTION_ENGINE ->
				ExecutionEngineIntegrationReason.PARTIAL_EXECUTION_ENGINE;
			case NOT_READY -> ExecutionEngineIntegrationReason.NOT_READY_EXECUTION_ENGINE;
			case UNRELIABLE -> ExecutionEngineIntegrationReason.UNRELIABLE_EXECUTION_ENGINE;
			case BLOCKED -> ExecutionEngineIntegrationReason.BLOCKED_EXECUTION_ENGINE;
			case UNKNOWN -> ExecutionEngineIntegrationReason.UNKNOWN;
		};
	}

	private ExecutionDispatchIntegrationResult dispatchReadyView() {
		return new ExecutionDispatchIntegrationResult(
				dispatch(),
				ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW,
				ExecutionDispatchIntegrationReason.DISPATCH_READY,
				ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionDispatch dispatch() {
		return new ExecutionDispatch(
				ExecutionDispatchLevel.DISPATCH_READY,
				ExecutionDispatchReason.EXECUTION_PLAN_READY,
				ExecutionDispatchScope.EXECUTION_DISPATCH,
				executionPlanReadyView(),
				DISPATCH_IDENTIFIER,
				EXECUTION_ENDPOINT,
				DISPATCH_POLICY,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionPlanIntegrationResult executionPlanReadyView() {
		return new ExecutionPlanIntegrationResult(
				executionPlan(),
				ExecutionPlanIntegrationStatus.EXECUTION_PLAN_READY_VIEW,
				ExecutionPlanIntegrationReason.EXECUTION_PLAN_READY,
				ExecutionPlanIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionPlan executionPlan() {
		return new ExecutionPlan(
				ExecutionPlanLevel.EXECUTION_PLAN_READY,
				ExecutionPlanReason.EXECUTION_PERMISSION_READY,
				ExecutionPlanScope.EXECUTION_PLAN,
				executionPermissionReady(),
				EXECUTION_PLAN_IDENTIFIER,
				EXECUTION_SEQUENCE,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionPermissionIntegrationResult executionPermissionReady() {
		return new ExecutionPermissionIntegrationResult(
				new ExecutionPermission(
						ExecutionPermissionLevel.EXECUTION_PERMITTED,
						ExecutionPermissionReason.ACTION_COMMAND_CANDIDATE_READY,
						ExecutionPermissionScope.EXECUTION_PERMISSION,
						actionCommandCandidateReady(),
						EXECUTION_PERMISSION_IDENTIFIER,
						"policy/manual-execution-gate",
						OPERATOR_AUTHORIZATION,
						true,
						OperationalUncertainty.LOW,
						false
				),
				ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY,
				ExecutionPermissionIntegrationReason.EXECUTION_PERMITTED,
				ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ActionCommandIntegrationResult actionCommandCandidateReady() {
		return new ActionCommandIntegrationResult(
				actionCommand(),
				ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY,
				ActionCommandIntegrationReason.ACTION_COMMAND_READY,
				ActionCommandIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ActionCommand actionCommand() {
		return new ActionCommand(
				ActionCommandLevel.ACTION_COMMAND_READY,
				ActionCommandReason.VERIFICATION_REQUEST_READY,
				ActionCommandScope.ACTION_COMMAND,
				verificationRequestReady(),
				ACTION_COMMAND_IDENTIFIER,
				ACTION_TYPE,
				TARGET_LAYER,
				BLAST_RADIUS,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private VerificationRequestIntegrationResult verificationRequestReady() {
		return new VerificationRequestIntegrationResult(
				verificationRequest(),
				VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY,
				VerificationRequestIntegrationReason.VERIFICATION_REQUESTABLE,
				VerificationRequestIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private VerificationRequest verificationRequest() {
		return new VerificationRequest(
				VerificationRequestLevel.VERIFICATION_REQUESTABLE,
				VerificationRequestReason.APPROVAL_DECISION_PENDING_VIEW,
				VerificationRequestScope.APPROVAL_DECISION,
				approvalDecisionPendingView(),
				VERIFICATION_REQUEST_IDENTIFIER,
				VERIFICATION_POLICY,
				true,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalDecisionIntegrationResult approvalDecisionPendingView() {
		return new ApprovalDecisionIntegrationResult(
				approvalDecision(),
				ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW,
				ApprovalDecisionIntegrationReason.DECISION_PENDING,
				ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalDecision approvalDecision() {
		return new ApprovalDecision(
				ApprovalDecisionLevel.DECISION_PENDING,
				ApprovalDecisionReason.APPROVAL_PENDING_VIEW,
				ApprovalDecisionScope.APPROVAL_STATE,
				approvalPendingView(),
				DECISION_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				true,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalStateIntegrationResult approvalPendingView() {
		return new ApprovalStateIntegrationResult(
				approvalState(),
				ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW,
				ApprovalStateIntegrationReason.PENDING_APPROVAL_STATE,
				ApprovalStateIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalState approvalState() {
		return new ApprovalState(
				ApprovalStateLevel.PENDING_APPROVAL,
				ApprovalStateReason.APPROVAL_REQUEST_READY,
				ApprovalStateScope.APPROVAL_REQUEST,
				approvalRequestReady(),
				APPROVAL_STATE_IDENTIFIER,
				APPROVAL_POLICY,
				OPERATOR_CONTEXT,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ApprovalRequestIntegrationResult approvalRequestReady() {
		return new ApprovalRequestIntegrationResult(
				approvalRequest(),
				ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY,
				ApprovalRequestIntegrationReason.REQUESTABLE_APPROVAL_REQUEST,
				ApprovalRequestIntegrationScope.APPROVAL_REQUEST,
				true,
				true
		);
	}

	private ApprovalRequest approvalRequest() {
		return new ApprovalRequest(
				ApprovalRequestLevel.REQUESTABLE,
				ApprovalRequestReason.EXPOSABLE_PRESENTATION,
				ApprovalRequestScope.APPROVAL_REQUEST,
				exposablePresentation(),
				OPERATOR_CONTEXT,
				true,
				APPROVAL_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private RecommendationPresentationIntegrationResult exposablePresentation() {
		return new RecommendationPresentationIntegrationResult(
				presentation(),
				RecommendationPresentationIntegrationStatus.EXPOSABLE,
				RecommendationPresentationIntegrationReason.VALID_RECOMMENDATION_PRESENTATION,
				RecommendationPresentationIntegrationScope.RECOMMENDATION,
				true,
				true
		);
	}

	private RecommendationPresentation presentation() {
		return new RecommendationPresentation(
				"rec-001",
				"Mitigate payment latency degradation",
				"Use the matched runbook with rollback and verification references.",
				RecommendationModelType.INCIDENT_RESPONSE,
				RecommendationModelReason.SCENARIO_MATCH,
				"scenario/payments-degradation",
				"runbook/payment-latency-mitigation",
				"rollback/payments",
				"verification/payments",
				"evidence/payment-latency-correlation",
				"PAYMENT_SAFE_REVIEWED",
				PRESENTED_AT,
				RecommendationPresentationStatus.PRESENTABLE,
				RecommendationPresentationReason.VALID_RECOMMENDATION,
				RecommendationPresentationScope.PRESENTATION
		);
	}
}
