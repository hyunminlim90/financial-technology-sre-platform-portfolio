package com.fintech.sre.agent.runtime.reliability;

public class ScenarioBinding {

	public ScenarioBindingDecision bind(ScenarioReference scenarioReference) {
		if (scenarioReference == null) {
			return rejected(ScenarioBindingRejectionReason.MISSING_SCENARIO_REFERENCE);
		}
		if (!scenarioReference.known()) {
			return new ScenarioBindingDecision(
					ScenarioBindingStatus.REJECTED,
					scenarioReference,
					ScenarioBindingRejectionReason.UNKNOWN_SCENARIO
			);
		}
		if (scenarioReference.deprecated()) {
			return new ScenarioBindingDecision(
					ScenarioBindingStatus.RESTRICTED,
					scenarioReference,
					ScenarioBindingRejectionReason
							.DEPRECATED_SCENARIO_HIGH_RISK_RESTRICTION
			);
		}
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.BOUND,
				scenarioReference,
				null
		);
	}

	private ScenarioBindingDecision rejected(
			ScenarioBindingRejectionReason rejectionReason
	) {
		return new ScenarioBindingDecision(
				ScenarioBindingStatus.REJECTED,
				null,
				rejectionReason
		);
	}
}
