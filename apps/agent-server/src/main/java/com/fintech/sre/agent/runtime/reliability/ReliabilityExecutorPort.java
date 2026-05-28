package com.fintech.sre.agent.runtime.reliability;

public interface ReliabilityExecutorPort {

	ExecutorResponse execute(ExecutorRequest request);
}
