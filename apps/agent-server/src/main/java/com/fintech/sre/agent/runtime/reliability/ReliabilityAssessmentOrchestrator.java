package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReliabilityAssessmentOrchestrator {

	private final VerificationGate verificationGate;

	public ReliabilityAssessmentOrchestrator(
			VerificationGate verificationGate
	) {
		this.verificationGate = Objects.requireNonNull(
				verificationGate,
				"verificationGate must not be null"
		);
	}

	public ReliabilityAssessmentResult assess(
			ReliabilityAssessmentInput input
	) {
		Objects.requireNonNull(input, "input must not be null");

		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				input.evidenceSignals(),
				input.contradictoryEvidence()
		);
		List<ReliabilityAssessmentStage> stages = new ArrayList<>();
		stages.add(ReliabilityAssessmentStage.EVIDENCE_CORRELATION);

		if (input.runtimeState() == RuntimeState.FAILED) {
			return new ReliabilityAssessmentResult(
					input.runtimeState(),
					stages,
					correlation,
					null,
					null,
					null,
					maxRisk(correlation.operationalUncertainty(), OperationalUncertainty.HIGH),
					ReliabilityAssessmentRejectionReason.FAILED_STATE_TERMINAL
			);
		}

		VerificationRequirement requirement = new VerificationRequirement(
				VerificationRequirementType.CONVERGED,
				correlation
		);
		VerificationGateDecision gateDecision = verificationGate.evaluate(
				requirement
		);
		stages.add(ReliabilityAssessmentStage.VERIFICATION_GATE);

		ConvergenceAssessment convergenceAssessment = new ConvergenceAssessment(
				input.runtimeState(),
				new ConvergenceEvidence(correlation, gateDecision),
				input.convergenceWindow(),
				input.propagationSignal(),
				input.propagationActive(),
				input.rollbackRecentlyApplied()
		);
		ConvergenceDecision convergenceDecision = ConvergenceDecision.evaluate(
				convergenceAssessment
		);
		stages.add(ReliabilityAssessmentStage.CONVERGENCE_ASSESSMENT);

		RegressionAssessment regressionAssessment = new RegressionAssessment(
				input.runtimeState(),
				convergenceStatus(input.runtimeState(), convergenceDecision),
				regressionSignals(input, correlation)
		);
		RegressionDecision regressionDecision = RegressionDecision.evaluate(
				regressionAssessment
		);
		stages.add(ReliabilityAssessmentStage.REGRESSION_ASSESSMENT);

		return new ReliabilityAssessmentResult(
				input.runtimeState(),
				stages,
				correlation,
				gateDecision,
				convergenceDecision,
				regressionDecision,
				overallRisk(correlation, regressionDecision),
				rejectionReason(
						correlation,
						gateDecision,
						convergenceDecision,
						regressionDecision
				)
		);
	}

	private ConvergenceStatus convergenceStatus(
			RuntimeState runtimeState,
			ConvergenceDecision convergenceDecision
	) {
		if (runtimeState == RuntimeState.CONVERGED
				|| convergenceDecision.converged()) {
			return ConvergenceStatus.CONVERGED;
		}
		return convergenceDecision.status();
	}

	private List<RegressionSignal> regressionSignals(
			ReliabilityAssessmentInput input,
			EvidenceCorrelation correlation
	) {
		List<RegressionSignal> signals = new ArrayList<>(input.regressionSignals());
		if (correlation.contradictoryEvidence()
				&& signals.stream().noneMatch(signal -> signal.type()
						== RegressionSignalType.CONTRADICTORY_EVIDENCE)) {
			signals.add(new RegressionSignal(
					RegressionSignalType.CONTRADICTORY_EVIDENCE,
					"derived-contradictory-evidence",
					"Contradictory evidence requires regression-first semantics."
			));
		}
		if (input.runtimeState() == RuntimeState.CONVERGED
				&& input.propagationActive()
				&& signals.stream().noneMatch(signal -> signal.type()
						== RegressionSignalType.PROPAGATION_REACTIVATED)) {
			signals.add(new RegressionSignal(
					RegressionSignalType.PROPAGATION_REACTIVATED,
					"derived-propagation-reactivated",
					"Propagation reactivation invalidates prior convergence."
			));
		}
		return List.copyOf(signals);
	}

	private OperationalUncertainty overallRisk(
			EvidenceCorrelation correlation,
			RegressionDecision regressionDecision
	) {
		OperationalUncertainty risk = correlation.operationalUncertainty();
		if (correlation.paymentSafetyUncertain()) {
			risk = maxRisk(risk, OperationalUncertainty.HIGH);
		}
		if (regressionDecision.regressionDetected()
				&& regressionDecision.severity() == RegressionSeverity.HIGH) {
			risk = maxRisk(risk, OperationalUncertainty.HIGH);
		}
		return risk;
	}

	private ReliabilityAssessmentRejectionReason rejectionReason(
			EvidenceCorrelation correlation,
			VerificationGateDecision gateDecision,
			ConvergenceDecision convergenceDecision,
			RegressionDecision regressionDecision
	) {
		if (regressionDecision.regressionDetected()) {
			if (correlation.contradictoryEvidence()) {
				return ReliabilityAssessmentRejectionReason
						.REGRESSION_PRIORITIZED_OVER_CONVERGENCE;
			}
			return ReliabilityAssessmentRejectionReason.REGRESSION_DETECTED;
		}
		if (!gateDecision.admitted()) {
			return ReliabilityAssessmentRejectionReason.VERIFICATION_GATE_REJECTED;
		}
		if (!convergenceDecision.converged()) {
			return ReliabilityAssessmentRejectionReason.CONVERGENCE_REJECTED;
		}
		return null;
	}

	private OperationalUncertainty maxRisk(
			OperationalUncertainty left,
			OperationalUncertainty right
	) {
		return left.ordinal() >= right.ordinal() ? left : right;
	}
}
