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

class OperationalReliabilityExecutionContextTest {

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
	private static final String ADAPTER_IDENTIFIER = "execution-adapter/payments/001";
	private static final String ADAPTER_TYPE = "KUBERNETES_ADAPTER_REFERENCE";
	private static final String ADAPTER_BINDING = "adapter://payments-prod/restart";
	private static final String ADAPTER_POLICY = "policy/manual-adapter-binding";
	private static final String EXECUTOR_IDENTIFIER = "execution-executor/payments/001";
	private static final String EXECUTION_STRATEGY = "MANUAL_GATED_RUNTIME_EXECUTION";
	private static final String EXECUTION_BOUNDARY = "runtime-boundary/payments-prod";
	private static final String EXECUTOR_POLICY = "policy/runtime-executor";
	private static final String SESSION_IDENTIFIER = "execution-session/payments/001";
	private static final String EXECUTION_CORRELATION_IDENTIFIER = "execution-correlation/payments/001";
	private static final String SESSION_EXECUTION_SCOPE = "execution-scope/payments-prod";
	private static final String SESSION_POLICY = "policy/runtime-execution-session";
	private static final String CONTEXT_IDENTIFIER = "execution-context/payments/001";
	private static final String EXECUTION_CONTEXT_SCOPE_VALUE = "context-scope/payments-runtime";
	private static final String EXECUTION_METADATA = "metadata=payments-prod,region=apne2";
	private static final String CONTEXT_POLICY = "policy/runtime-execution-context";
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-20T00:00:00Z");

	private final ExecutionContextEvaluator evaluator = new ExecutionContextEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.readOnly()).isTrue();
		assertThat(context.actualContextCreation()).isFalse();
		assertThat(context.threadLocalCreation()).isFalse();
		assertThat(context.securityContextCreation()).isFalse();
		assertThat(context.transactionContextCreation()).isFalse();
		assertThat(context.kubernetesContextCreation()).isFalse();
		assertThat(context.runtimeExecution()).isFalse();
		assertThat(context.actionExecution()).isFalse();
	}

	@Test
	void shouldBeExecutionContextReadyWhenSessionReadyAndRequirementsPresent() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.EXECUTION_CONTEXT_READY);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.EXECUTION_SESSION_READY);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.EXECUTION_CONTEXT);
	}

	@Test
	void shouldBlockWhenContextIdentifierMissing() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				" ",
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.MISSING_CONTEXT_IDENTIFIER);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.EXECUTION_CONTEXT);
	}

	@Test
	void shouldBlockWhenExecutionContextScopeMissing() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				" ",
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason())
				.isEqualTo(ExecutionContextReason.MISSING_EXECUTION_CONTEXT_SCOPE);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.EXECUTION_CONTEXT_SCOPE);
	}

	@Test
	void shouldBlockWhenExecutionMetadataMissing() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				" ",
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.MISSING_EXECUTION_METADATA);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.EXECUTION_METADATA);
	}

	@Test
	void shouldBlockWhenContextPolicyMissing() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				" ",
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.MISSING_CONTEXT_POLICY);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.CONTEXT_POLICY);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason())
				.isEqualTo(ExecutionContextReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionContext context = evaluator.evaluate(
				sessionReadyView(),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(context.scope()).isEqualTo(ExecutionContextScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenSessionIsPartial() {
		ExecutionContext context = evaluator.evaluate(
				sessionWithStatus(ExecutionSessionIntegrationStatus.PARTIAL_EXECUTION_SESSION),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.PARTIAL);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.PARTIAL_EXECUTION_SESSION);
	}

	@Test
	void shouldRemainNotReadyWhenSessionIsNotReady() {
		ExecutionContext context = evaluator.evaluate(
				sessionWithStatus(ExecutionSessionIntegrationStatus.NOT_READY),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.NOT_READY);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.NOT_READY_EXECUTION_SESSION);
	}

	@Test
	void shouldRemainUnreliableWhenSessionIsUnreliable() {
		ExecutionContext context = evaluator.evaluate(
				sessionWithStatus(ExecutionSessionIntegrationStatus.UNRELIABLE),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.UNRELIABLE);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.UNRELIABLE_EXECUTION_SESSION);
	}

	@Test
	void shouldRemainBlockedWhenSessionIsBlocked() {
		ExecutionContext context = evaluator.evaluate(
				sessionWithStatus(ExecutionSessionIntegrationStatus.BLOCKED),
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(context.level()).isEqualTo(ExecutionContextLevel.BLOCKED);
		assertThat(context.reason()).isEqualTo(ExecutionContextReason.BLOCKED_EXECUTION_SESSION);
	}

	@Test
	void shouldRejectNullExecutionSessionIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				CONTEXT_IDENTIFIER,
				EXECUTION_CONTEXT_SCOPE_VALUE,
				EXECUTION_METADATA,
				CONTEXT_POLICY,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("executionSessionIntegration must not be null");
	}

	private ExecutionSessionIntegrationResult sessionReadyView() {
		return sessionWithStatus(
				ExecutionSessionIntegrationStatus.EXECUTION_SESSION_READY_VIEW
		);
	}

	private ExecutionSessionIntegrationResult sessionWithStatus(
			ExecutionSessionIntegrationStatus status
	) {
		return new ExecutionSessionIntegrationResult(
				session(status),
				status,
				sessionIntegrationReason(status),
				ExecutionSessionIntegrationScope.OPERATOR_VIEW,
				status == ExecutionSessionIntegrationStatus.EXECUTION_SESSION_READY_VIEW,
				status == ExecutionSessionIntegrationStatus.EXECUTION_SESSION_READY_VIEW
		);
	}

	private ExecutionSession session(ExecutionSessionIntegrationStatus status) {
		return new ExecutionSession(
				sessionLevel(status),
				sessionReason(status),
				ExecutionSessionScope.EXECUTION_SESSION,
				executorReadyView(),
				SESSION_IDENTIFIER,
				EXECUTION_CORRELATION_IDENTIFIER,
				SESSION_EXECUTION_SCOPE,
				SESSION_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionSessionLevel sessionLevel(ExecutionSessionIntegrationStatus status) {
		return switch (status) {
			case EXECUTION_SESSION_READY_VIEW ->
				ExecutionSessionLevel.EXECUTION_SESSION_READY;
			case PARTIAL_EXECUTION_SESSION -> ExecutionSessionLevel.PARTIAL;
			case NOT_READY -> ExecutionSessionLevel.NOT_READY;
			case UNRELIABLE -> ExecutionSessionLevel.UNRELIABLE;
			case BLOCKED -> ExecutionSessionLevel.BLOCKED;
			case UNKNOWN -> ExecutionSessionLevel.UNKNOWN;
		};
	}

	private ExecutionSessionReason sessionReason(ExecutionSessionIntegrationStatus status) {
		return switch (status) {
			case EXECUTION_SESSION_READY_VIEW ->
				ExecutionSessionReason.EXECUTION_EXECUTOR_READY;
			case PARTIAL_EXECUTION_SESSION ->
				ExecutionSessionReason.PARTIAL_EXECUTION_EXECUTOR;
			case NOT_READY ->
				ExecutionSessionReason.NOT_READY_EXECUTION_EXECUTOR;
			case UNRELIABLE ->
				ExecutionSessionReason.UNRELIABLE_EXECUTION_EXECUTOR;
			case BLOCKED ->
				ExecutionSessionReason.BLOCKED_EXECUTION_EXECUTOR;
			case UNKNOWN -> ExecutionSessionReason.UNKNOWN;
		};
	}

	private ExecutionSessionIntegrationReason sessionIntegrationReason(
			ExecutionSessionIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_SESSION_READY_VIEW ->
				ExecutionSessionIntegrationReason.EXECUTION_SESSION_READY;
			case PARTIAL_EXECUTION_SESSION ->
				ExecutionSessionIntegrationReason.PARTIAL_EXECUTION_SESSION;
			case NOT_READY ->
				ExecutionSessionIntegrationReason.NOT_READY_EXECUTION_SESSION;
			case UNRELIABLE ->
				ExecutionSessionIntegrationReason.UNRELIABLE_EXECUTION_SESSION;
			case BLOCKED ->
				ExecutionSessionIntegrationReason.BLOCKED_EXECUTION_SESSION;
			case UNKNOWN -> ExecutionSessionIntegrationReason.UNKNOWN;
		};
	}

	private ExecutionExecutorIntegrationResult executorReadyView() {
		return executorWithStatus(
				ExecutionExecutorIntegrationStatus.EXECUTION_EXECUTOR_READY_VIEW
		);
	}

	private ExecutionExecutorIntegrationResult executorWithStatus(
			ExecutionExecutorIntegrationStatus status
	) {
		return new ExecutionExecutorIntegrationResult(
				executor(status),
				status,
				executorIntegrationReason(status),
				ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
				status == ExecutionExecutorIntegrationStatus.EXECUTION_EXECUTOR_READY_VIEW,
				status == ExecutionExecutorIntegrationStatus.EXECUTION_EXECUTOR_READY_VIEW
		);
	}

	private ExecutionExecutor executor(ExecutionExecutorIntegrationStatus status) {
		return new ExecutionExecutor(
				executorLevel(status),
				executorReason(status),
				ExecutionExecutorScope.EXECUTION_EXECUTOR,
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionExecutorLevel executorLevel(ExecutionExecutorIntegrationStatus status) {
		return switch (status) {
			case EXECUTION_EXECUTOR_READY_VIEW ->
				ExecutionExecutorLevel.EXECUTION_EXECUTOR_READY;
			case PARTIAL_EXECUTION_EXECUTOR -> ExecutionExecutorLevel.PARTIAL;
			case NOT_READY -> ExecutionExecutorLevel.NOT_READY;
			case UNRELIABLE -> ExecutionExecutorLevel.UNRELIABLE;
			case BLOCKED -> ExecutionExecutorLevel.BLOCKED;
			case UNKNOWN -> ExecutionExecutorLevel.UNKNOWN;
		};
	}

	private ExecutionExecutorReason executorReason(ExecutionExecutorIntegrationStatus status) {
		return switch (status) {
			case EXECUTION_EXECUTOR_READY_VIEW ->
				ExecutionExecutorReason.EXECUTION_ADAPTER_READY;
			case PARTIAL_EXECUTION_EXECUTOR ->
				ExecutionExecutorReason.PARTIAL_EXECUTION_ADAPTER;
			case NOT_READY ->
				ExecutionExecutorReason.NOT_READY_EXECUTION_ADAPTER;
			case UNRELIABLE ->
				ExecutionExecutorReason.UNRELIABLE_EXECUTION_ADAPTER;
			case BLOCKED ->
				ExecutionExecutorReason.BLOCKED_EXECUTION_ADAPTER;
			case UNKNOWN -> ExecutionExecutorReason.UNKNOWN;
		};
	}

	private ExecutionExecutorIntegrationReason executorIntegrationReason(
			ExecutionExecutorIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_EXECUTOR_READY_VIEW ->
				ExecutionExecutorIntegrationReason.EXECUTION_EXECUTOR_READY;
			case PARTIAL_EXECUTION_EXECUTOR ->
				ExecutionExecutorIntegrationReason.PARTIAL_EXECUTION_EXECUTOR;
			case NOT_READY ->
				ExecutionExecutorIntegrationReason.NOT_READY_EXECUTION_EXECUTOR;
			case UNRELIABLE ->
				ExecutionExecutorIntegrationReason.UNRELIABLE_EXECUTION_EXECUTOR;
			case BLOCKED ->
				ExecutionExecutorIntegrationReason.BLOCKED_EXECUTION_EXECUTOR;
			case UNKNOWN -> ExecutionExecutorIntegrationReason.UNKNOWN;
		};
	}

	private ExecutionAdapterIntegrationResult adapterReadyView() {
		return new ExecutionAdapterIntegrationResult(
				new ExecutionAdapter(
						ExecutionAdapterLevel.EXECUTION_ADAPTER_READY,
						ExecutionAdapterReason.EXECUTION_ENGINE_SELECTOR_READY,
						ExecutionAdapterScope.EXECUTION_ADAPTER,
						selectorReadyView(),
						ADAPTER_IDENTIFIER,
						ADAPTER_TYPE,
						ADAPTER_BINDING,
						ADAPTER_POLICY,
						OperationalUncertainty.LOW,
						false
				),
				ExecutionAdapterIntegrationStatus.EXECUTION_ADAPTER_READY_VIEW,
				ExecutionAdapterIntegrationReason.EXECUTION_ADAPTER_READY,
				ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionEngineSelectorIntegrationResult selectorReadyView() {
		return new ExecutionEngineSelectorIntegrationResult(
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
						false
				),
				ExecutionEngineSelectorIntegrationStatus.EXECUTION_ENGINE_SELECTOR_READY_VIEW,
				ExecutionEngineSelectorIntegrationReason.EXECUTION_ENGINE_SELECTOR_READY,
				ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionEngineRegistryIntegrationResult registryReadyView() {
		return new ExecutionEngineRegistryIntegrationResult(
				new ExecutionEngineRegistry(
						ExecutionEngineRegistryLevel.EXECUTION_ENGINE_REGISTRY_READY,
						ExecutionEngineRegistryReason.EXECUTION_ENGINE_READY,
						ExecutionEngineRegistryScope.EXECUTION_ENGINE_REGISTRY,
						executionEngineReadyView(),
						REGISTRY_IDENTIFIER,
						true,
						REGISTRY_POLICY,
						true,
						OperationalUncertainty.LOW,
						false
				),
				ExecutionEngineRegistryIntegrationStatus.EXECUTION_ENGINE_REGISTRY_READY_VIEW,
				ExecutionEngineRegistryIntegrationReason.EXECUTION_ENGINE_REGISTRY_READY,
				ExecutionEngineRegistryIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionEngineIntegrationResult executionEngineReadyView() {
		return new ExecutionEngineIntegrationResult(
				new ExecutionEngine(
						ExecutionEngineLevel.EXECUTION_ENGINE_READY,
						ExecutionEngineReason.DISPATCH_READY,
						ExecutionEngineScope.EXECUTION_ENGINE,
						dispatchReadyView(),
						EXECUTION_ENGINE_IDENTIFIER,
						EXECUTION_ENGINE_TYPE,
						EXECUTION_ENDPOINT,
						EXECUTION_POLICY,
						OperationalUncertainty.LOW,
						false
				),
				ExecutionEngineIntegrationStatus.EXECUTION_ENGINE_READY_VIEW,
				ExecutionEngineIntegrationReason.EXECUTION_ENGINE_READY,
				ExecutionEngineIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionDispatchIntegrationResult dispatchReadyView() {
		return new ExecutionDispatchIntegrationResult(
				new ExecutionDispatch(
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
				),
				ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW,
				ExecutionDispatchIntegrationReason.DISPATCH_READY,
				ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ExecutionPlanIntegrationResult executionPlanReadyView() {
		return new ExecutionPlanIntegrationResult(
				new ExecutionPlan(
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
				),
				ExecutionPlanIntegrationStatus.EXECUTION_PLAN_READY_VIEW,
				ExecutionPlanIntegrationReason.EXECUTION_PLAN_READY,
				ExecutionPlanIntegrationScope.OPERATOR_VIEW,
				true,
				true
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
						OPERATOR_AUTHORIZATION,
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
				new ActionCommand(
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
				),
				ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY,
				ActionCommandIntegrationReason.ACTION_COMMAND_READY,
				ActionCommandIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private VerificationRequestIntegrationResult verificationRequestReady() {
		return new VerificationRequestIntegrationResult(
				new VerificationRequest(
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
				),
				VerificationRequestIntegrationStatus.VERIFICATION_REQUEST_READY,
				VerificationRequestIntegrationReason.VERIFICATION_REQUESTABLE,
				VerificationRequestIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalDecisionIntegrationResult approvalDecisionPendingView() {
		return new ApprovalDecisionIntegrationResult(
				new ApprovalDecision(
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
				),
				ApprovalDecisionIntegrationStatus.APPROVAL_DECISION_PENDING_VIEW,
				ApprovalDecisionIntegrationReason.DECISION_PENDING,
				ApprovalDecisionIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalStateIntegrationResult approvalPendingView() {
		return new ApprovalStateIntegrationResult(
				new ApprovalState(
						ApprovalStateLevel.PENDING_APPROVAL,
						ApprovalStateReason.APPROVAL_REQUEST_READY,
						ApprovalStateScope.APPROVAL_REQUEST,
						approvalRequestReady(),
						APPROVAL_STATE_IDENTIFIER,
						APPROVAL_POLICY,
						OPERATOR_CONTEXT,
						OperationalUncertainty.LOW,
						false
				),
				ApprovalStateIntegrationStatus.APPROVAL_PENDING_VIEW,
				ApprovalStateIntegrationReason.PENDING_APPROVAL_STATE,
				ApprovalStateIntegrationScope.OPERATOR_VIEW,
				true,
				true
		);
	}

	private ApprovalRequestIntegrationResult approvalRequestReady() {
		return new ApprovalRequestIntegrationResult(
				new ApprovalRequest(
						ApprovalRequestLevel.REQUESTABLE,
						ApprovalRequestReason.EXPOSABLE_PRESENTATION,
						ApprovalRequestScope.APPROVAL_REQUEST,
						exposablePresentation(),
						OPERATOR_CONTEXT,
						true,
						APPROVAL_POLICY,
						OperationalUncertainty.LOW,
						false
				),
				ApprovalRequestIntegrationStatus.APPROVAL_REQUEST_READY,
				ApprovalRequestIntegrationReason.REQUESTABLE_APPROVAL_REQUEST,
				ApprovalRequestIntegrationScope.APPROVAL_REQUEST,
				true,
				true
		);
	}

	private RecommendationPresentationIntegrationResult exposablePresentation() {
		return new RecommendationPresentationIntegrationResult(
				new RecommendationPresentation(
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
				),
				RecommendationPresentationIntegrationStatus.EXPOSABLE,
				RecommendationPresentationIntegrationReason.VALID_RECOMMENDATION_PRESENTATION,
				RecommendationPresentationIntegrationScope.RECOMMENDATION,
				true,
				true
		);
	}
}
