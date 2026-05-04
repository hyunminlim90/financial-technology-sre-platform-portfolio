package com.fintech.sre.agent.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.actionlog.service.ActionLogService;
import com.fintech.sre.agent.actionlog.dto.RecordExecutedActionRequest;
import com.fintech.sre.agent.actionlog.dto.RecordRollbackRequest;
import com.fintech.sre.agent.actionlog.dto.RecordVerificationRequest;
import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.RollbackRecordEntity;
import com.fintech.sre.agent.actionlog.entity.VerificationResultEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class ActionLogController {

	private final ActionLogService actionLogService;

	@PostMapping("/{incidentId}/actions")
	public Mono<ExecutedActionEntity> recordAction(
			@PathVariable String incidentId,
			@Valid @RequestBody RecordExecutedActionRequest request
	) {
		return actionLogService.recordExecutedAction(incidentId, request);
	}

	@PostMapping("/{incidentId}/actions/{actionId}/verification")
	public Mono<VerificationResultEntity> recordVerification(
			@PathVariable String incidentId,
			@PathVariable Long actionId,
			@Valid @RequestBody RecordVerificationRequest request
	) {
		return actionLogService.recordVerification(incidentId, actionId, request);
	}

	@PostMapping("/{incidentId}/actions/{actionId}/rollback")
	public Mono<RollbackRecordEntity> recordRollback(
			@PathVariable String incidentId,
			@PathVariable Long actionId,
			@Valid @RequestBody RecordRollbackRequest request
	) {
		return actionLogService.recordRollback(incidentId, actionId, request);
	}
}
