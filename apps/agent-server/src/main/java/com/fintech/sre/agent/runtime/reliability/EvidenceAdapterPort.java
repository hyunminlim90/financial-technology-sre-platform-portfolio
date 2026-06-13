package com.fintech.sre.agent.runtime.reliability;

public interface EvidenceAdapterPort {

	EvidenceQueryResult collect(EvidenceQuery query);
}
