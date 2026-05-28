package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRollbackVerificationBindingSkeletonTest {

	private final RollbackVerificationBinding binding =
			new RollbackVerificationBinding();

	@Test
	void shouldRejectWhenRollbackReferenceIsMissing() {
		RollbackVerificationBindingDecision decision = binding.bind(
				null,
				verificationReference(true, false, true),
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.REJECTED
		);
		assertThat(decision.actionCommandAvailable()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE
		);
	}

	@Test
	void shouldRejectWhenVerificationReferenceIsMissing() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				null,
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.REJECTED
		);
		assertThat(decision.actionCommandAvailable()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE
		);
	}

	@Test
	void shouldRemainSemanticPrerequisiteOnly() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(true, false, true),
				false
		);

		assertThat(decision.semanticPrerequisiteOnly()).isTrue();
		assertThat(decision.executionPermission()).isFalse();
	}

	@Test
	void shouldRejectUnknownRollbackReference() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(false, false),
				verificationReference(true, false, true),
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason.UNKNOWN_ROLLBACK_REFERENCE
		);
	}

	@Test
	void shouldRejectUnknownVerificationReference() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(false, false, true),
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.REJECTED
		);
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason.UNKNOWN_VERIFICATION_REFERENCE
		);
	}

	@Test
	void shouldRestrictDeprecatedRollbackAsHighRisk() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, true),
				verificationReference(true, false, true),
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.RESTRICTED
		);
		assertThat(decision.highRiskRestricted()).isTrue();
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason
						.DEPRECATED_ROLLBACK_HIGH_RISK_RESTRICTION
		);
	}

	@Test
	void shouldRestrictDeprecatedVerificationAsHighRisk() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(true, true, true),
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.RESTRICTED
		);
		assertThat(decision.highRiskRestricted()).isTrue();
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason
						.DEPRECATED_VERIFICATION_HIGH_RISK_RESTRICTION
		);
	}

	@Test
	void shouldRequirePaymentConsistencyVerificationForPaymentSafetyAction() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(true, false, false),
				true
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.REJECTED
		);
		assertThat(decision.actionCommandAvailable()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				RollbackVerificationBindingRejectionReason
						.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
		);
	}

	@Test
	void shouldBindKnownReferencesForNonPaymentAction() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(true, false, false),
				false
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.BOUND
		);
		assertThat(decision.actionCommandAvailable()).isTrue();
		assertThat(decision.rejectionReason()).isNull();
	}

	@Test
	void shouldBindKnownPaymentConsistencyVerificationForPaymentSafetyAction() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(true, false, true),
				true
		);

		assertThat(decision.status()).isEqualTo(
				RollbackVerificationBindingStatus.BOUND
		);
		assertThat(decision.actionCommandAvailable()).isTrue();
		assertThat(decision.rejectionReason()).isNull();
	}

	@Test
	void shouldReferencePortfolioKnowledgeWithoutMutatingIt() {
		RollbackVerificationBindingDecision decision = binding.bind(
				rollbackReference(true, false),
				verificationReference(true, false, true),
				false
		);

		assertThat(decision.rollbackReference().knowledgeSourceId())
				.isEqualTo("portfolio-runtime");
		assertThat(decision.verificationReference().knowledgeSourceId())
				.isEqualTo("portfolio-runtime");
		assertThat(decision.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullRollbackIdAtReferenceConstruction() {
		assertThatThrownBy(() -> new RollbackReference(
				null,
				"portfolio-runtime",
				true,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("rollbackId must not be null");
	}

	@Test
	void shouldRejectNullVerificationIdAtReferenceConstruction() {
		assertThatThrownBy(() -> new VerificationReference(
				null,
				"portfolio-runtime",
				true,
				false,
				true
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("verificationId must not be null");
	}

	private RollbackReference rollbackReference(boolean known, boolean deprecated) {
		return new RollbackReference(
				"rollback-1",
				"portfolio-runtime",
				known,
				deprecated
		);
	}

	private VerificationReference verificationReference(
			boolean known,
			boolean deprecated,
			boolean paymentConsistencyVerification
	) {
		return new VerificationReference(
				"verification-1",
				"portfolio-runtime",
				known,
				deprecated,
				paymentConsistencyVerification
		);
	}
}
