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

class OperationalReliabilityExecutionExecutorTest {

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
	private static final Instant PRESENTED_AT = Instant.parse("2026-06-20T00:00:00Z");

	private final ExecutionExecutorEvaluator evaluator = new ExecutionExecutorEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonExecutable() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.readOnly()).isTrue();
		assertThat(executor.executorImplementation()).isFalse();
		assertThat(executor.executorThreadCreation()).isFalse();
		assertThat(executor.adapterInvocation()).isFalse();
		assertThat(executor.kubernetesApiCall()).isFalse();
		assertThat(executor.kubectlExecution()).isFalse();
		assertThat(executor.argoCdSync()).isFalse();
		assertThat(executor.terraformOrOpenTofuApply()).isFalse();
		assertThat(executor.sshExecution()).isFalse();
		assertThat(executor.ansibleExecution()).isFalse();
		assertThat(executor.actionExecution()).isFalse();
		assertThat(executor.executionEngineExecution()).isFalse();
	}

	@Test
	void shouldBeExecutionExecutorReadyWhenAdapterReadyAndRequirementsPresent() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.EXECUTION_EXECUTOR_READY);
		assertThat(executor.reason()).isEqualTo(ExecutionExecutorReason.EXECUTION_ADAPTER_READY);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.EXECUTION_EXECUTOR);
	}

	@Test
	void shouldBlockWhenExecutorIdentifierMissing() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				" ",
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.MISSING_EXECUTOR_IDENTIFIER);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.EXECUTION_EXECUTOR);
	}

	@Test
	void shouldBlockWhenExecutionStrategyMissing() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				" ",
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.MISSING_EXECUTION_STRATEGY);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.EXECUTION_STRATEGY);
	}

	@Test
	void shouldBlockWhenExecutionBoundaryMissing() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				" ",
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.MISSING_EXECUTION_BOUNDARY);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.EXECUTION_BOUNDARY);
	}

	@Test
	void shouldBlockWhenExecutorPolicyMissing() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				" ",
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.MISSING_EXECUTOR_POLICY);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.EXECUTOR_POLICY);
	}

	@Test
	void shouldBlockWhenPaymentSafetyUncertaintyExists() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				true
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.PAYMENT_SAFETY);
	}

	@Test
	void shouldBlockWhenLifecycleRiskIsCritical() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterReadyView(),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.CRITICAL,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.CRITICAL_LIFECYCLE_RISK);
		assertThat(executor.scope()).isEqualTo(ExecutionExecutorScope.LIFECYCLE_RISK);
	}

	@Test
	void shouldRemainPartialWhenAdapterIsPartial() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterWithStatus(ExecutionAdapterIntegrationStatus.PARTIAL_EXECUTION_ADAPTER),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.PARTIAL);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.PARTIAL_EXECUTION_ADAPTER);
	}

	@Test
	void shouldRemainNotReadyWhenAdapterIsNotReady() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterWithStatus(ExecutionAdapterIntegrationStatus.NOT_READY),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.NOT_READY);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.NOT_READY_EXECUTION_ADAPTER);
	}

	@Test
	void shouldRemainUnreliableWhenAdapterIsUnreliable() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterWithStatus(ExecutionAdapterIntegrationStatus.UNRELIABLE),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.UNRELIABLE);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.UNRELIABLE_EXECUTION_ADAPTER);
	}

	@Test
	void shouldRemainBlockedWhenAdapterIsBlocked() {
		ExecutionExecutor executor = evaluator.evaluate(
				adapterWithStatus(ExecutionAdapterIntegrationStatus.BLOCKED),
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		);

		assertThat(executor.level()).isEqualTo(ExecutionExecutorLevel.BLOCKED);
		assertThat(executor.reason())
				.isEqualTo(ExecutionExecutorReason.BLOCKED_EXECUTION_ADAPTER);
	}

	@Test
	void shouldRejectNullExecutionAdapterIntegration() {
		assertThatThrownBy(() -> evaluator.evaluate(
				null,
				EXECUTOR_IDENTIFIER,
				EXECUTION_STRATEGY,
				EXECUTION_BOUNDARY,
				EXECUTOR_POLICY,
				OperationalUncertainty.LOW,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("executionAdapterIntegration must not be null");
	}

	private ExecutionAdapterIntegrationResult adapterReadyView() {
		return adapterWithStatus(
				ExecutionAdapterIntegrationStatus.EXECUTION_ADAPTER_READY_VIEW
		);
	}

	private ExecutionAdapterIntegrationResult adapterWithStatus(
			ExecutionAdapterIntegrationStatus status
	) {
		return new ExecutionAdapterIntegrationResult(
				adapter(status),
				status,
				adapterIntegrationReason(status),
				ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
				status == ExecutionAdapterIntegrationStatus.EXECUTION_ADAPTER_READY_VIEW,
				status == ExecutionAdapterIntegrationStatus.EXECUTION_ADAPTER_READY_VIEW
		);
	}

	private ExecutionAdapter adapter(ExecutionAdapterIntegrationStatus status) {
		return new ExecutionAdapter(
				adapterLevel(status),
				adapterReason(status),
				ExecutionAdapterScope.EXECUTION_ADAPTER,
				selectorReadyView(),
				ADAPTER_IDENTIFIER,
				ADAPTER_TYPE,
				ADAPTER_BINDING,
				ADAPTER_POLICY,
				OperationalUncertainty.LOW,
				false
		);
	}

	private ExecutionAdapterLevel adapterLevel(ExecutionAdapterIntegrationStatus status) {
		return switch (status) {
			case EXECUTION_ADAPTER_READY_VIEW ->
				ExecutionAdapterLevel.EXECUTION_ADAPTER_READY;
			case PARTIAL_EXECUTION_ADAPTER -> ExecutionAdapterLevel.PARTIAL;
			case NOT_READY -> ExecutionAdapterLevel.NOT_READY;
			case UNRELIABLE -> ExecutionAdapterLevel.UNRELIABLE;
			case BLOCKED -> ExecutionAdapterLevel.BLOCKED;
			case UNKNOWN -> ExecutionAdapterLevel.UNKNOWN;
		};
	}

	private ExecutionAdapterReason adapterReason(ExecutionAdapterIntegrationStatus status) {
		return switch (status) {
			case EXECUTION_ADAPTER_READY_VIEW ->
				ExecutionAdapterReason.EXECUTION_ENGINE_SELECTOR_READY;
			case PARTIAL_EXECUTION_ADAPTER ->
				ExecutionAdapterReason.PARTIAL_EXECUTION_ENGINE_SELECTOR;
			case NOT_READY ->
				ExecutionAdapterReason.NOT_READY_EXECUTION_ENGINE_SELECTOR;
			case UNRELIABLE ->
				ExecutionAdapterReason.UNRELIABLE_EXECUTION_ENGINE_SELECTOR;
			case BLOCKED ->
				ExecutionAdapterReason.BLOCKED_EXECUTION_ENGINE_SELECTOR;
			case UNKNOWN -> ExecutionAdapterReason.UNKNOWN;
		};
	}

	private ExecutionAdapterIntegrationReason adapterIntegrationReason(
			ExecutionAdapterIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ADAPTER_READY_VIEW ->
				ExecutionAdapterIntegrationReason.EXECUTION_ADAPTER_READY;
			case PARTIAL_EXECUTION_ADAPTER ->
				ExecutionAdapterIntegrationReason.PARTIAL_EXECUTION_ADAPTER;
			case NOT_READY ->
				ExecutionAdapterIntegrationReason.NOT_READY_EXECUTION_ADAPTER;
			case UNRELIABLE ->
				ExecutionAdapterIntegrationReason.UNRELIABLE_EXECUTION_ADAPTER;
			case BLOCKED ->
				ExecutionAdapterIntegrationReason.BLOCKED_EXECUTION_ADAPTER;
			case UNKNOWN -> ExecutionAdapterIntegrationReason.UNKNOWN;
		};
	}

	private ExecutionEngineSelectorIntegrationResult selectorReadyView() {
		return selectorWithStatus(
				ExecutionEngineSelectorIntegrationStatus.EXECUTION_ENGINE_SELECTOR_READY_VIEW
		);
	}

	private ExecutionEngineSelectorIntegrationResult selectorWithStatus(
			ExecutionEngineSelectorIntegrationStatus status
	) {
		return new ExecutionEngineSelectorIntegrationResult(
				selector(status),
				status,
				selectorIntegrationReason(status),
				ExecutionEngineSelectorIntegrationScope.OPERATOR_VIEW,
				status == ExecutionEngineSelectorIntegrationStatus.EXECUTION_ENGINE_SELECTOR_READY_VIEW,
				status == ExecutionEngineSelectorIntegrationStatus.EXECUTION_ENGINE_SELECTOR_READY_VIEW
		);
	}

	private ExecutionEngineSelector selector(
			ExecutionEngineSelectorIntegrationStatus status
	) {
		return new ExecutionEngineSelector(
				selectorLevel(status),
				selectorReason(status),
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

	private ExecutionEngineSelectorLevel selectorLevel(
			ExecutionEngineSelectorIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_SELECTOR_READY_VIEW ->
				ExecutionEngineSelectorLevel.EXECUTION_ENGINE_SELECTOR_READY;
			case PARTIAL_EXECUTION_ENGINE_SELECTOR ->
				ExecutionEngineSelectorLevel.PARTIAL;
			case NOT_READY -> ExecutionEngineSelectorLevel.NOT_READY;
			case UNRELIABLE -> ExecutionEngineSelectorLevel.UNRELIABLE;
			case BLOCKED -> ExecutionEngineSelectorLevel.BLOCKED;
			case UNKNOWN -> ExecutionEngineSelectorLevel.UNKNOWN;
		};
	}

	private ExecutionEngineSelectorReason selectorReason(
			ExecutionEngineSelectorIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_SELECTOR_READY_VIEW ->
				ExecutionEngineSelectorReason.EXECUTION_ENGINE_REGISTRY_READY;
			case PARTIAL_EXECUTION_ENGINE_SELECTOR ->
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

	private ExecutionEngineSelectorIntegrationReason selectorIntegrationReason(
			ExecutionEngineSelectorIntegrationStatus status
	) {
		return switch (status) {
			case EXECUTION_ENGINE_SELECTOR_READY_VIEW ->
				ExecutionEngineSelectorIntegrationReason.EXECUTION_ENGINE_SELECTOR_READY;
			case PARTIAL_EXECUTION_ENGINE_SELECTOR ->
				ExecutionEngineSelectorIntegrationReason.PARTIAL_EXECUTION_ENGINE_SELECTOR;
			case NOT_READY ->
				ExecutionEngineSelectorIntegrationReason.NOT_READY_EXECUTION_ENGINE_SELECTOR;
			case UNRELIABLE ->
				ExecutionEngineSelectorIntegrationReason.UNRELIABLE_EXECUTION_ENGINE_SELECTOR;
			case BLOCKED ->
				ExecutionEngineSelectorIntegrationReason.BLOCKED_EXECUTION_ENGINE_SELECTOR;
			case UNKNOWN -> ExecutionEngineSelectorIntegrationReason.UNKNOWN;
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
