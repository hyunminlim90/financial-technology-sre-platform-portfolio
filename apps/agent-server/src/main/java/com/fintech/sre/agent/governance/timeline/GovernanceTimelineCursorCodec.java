package com.fintech.sre.agent.governance.timeline;

public interface GovernanceTimelineCursorCodec {

	String encode(GovernanceTimelineCursor cursor);

	GovernanceTimelineCursor decode(String encodedCursor);
}
