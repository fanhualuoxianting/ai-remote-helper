package com.airh.relay.service;

import com.airh.protocol.dto.CreateHelpRequestRequest;
import com.airh.protocol.dto.HelpRequestResponse;
import com.airh.protocol.dto.ReviewHelpRequestRequest;
import com.airh.protocol.enums.HelpRequestStatus;
import com.airh.relay.device.DeviceRegistry;
import com.airh.relay.domain.HelpRequestEntity;
import com.airh.relay.repository.HelpRequestRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HelpRequestService {
    private final HelpRequestRepository helpRequestRepository;
    private final DeviceRegistry deviceRegistry;
    private final AuditService auditService;

    public HelpRequestService(HelpRequestRepository helpRequestRepository, DeviceRegistry deviceRegistry,
                              AuditService auditService) {
        this.helpRequestRepository = helpRequestRepository;
        this.deviceRegistry = deviceRegistry;
        this.auditService = auditService;
    }

    public HelpRequestResponse create(String sessionId, CreateHelpRequestRequest request) {
        requireOnlineSession(sessionId);
        String content = request == null ? "" : safeText(request.content());
        if (content.isBlank()) {
            throw new IllegalArgumentException("需求内容不能为空");
        }
        if (content.length() > 8000) {
            throw new IllegalArgumentException("需求内容过长，请控制在 8000 字以内");
        }
        Instant now = Instant.now();
        HelpRequestEntity entity = new HelpRequestEntity(
                UUID.randomUUID().toString(),
                sessionId,
                content,
                HelpRequestStatus.PENDING,
                null,
                now,
                now,
                null
        );
        helpRequestRepository.save(entity);
        auditService.logEvent(sessionId, "HELP_REQUEST_CREATED", Map.of("requestId", entity.getRequestId()));
        return toResponse(entity);
    }

    public List<HelpRequestResponse> list(String sessionId) {
        requireOnlineSession(sessionId);
        return helpRequestRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public HelpRequestResponse approve(String sessionId, String requestId, ReviewHelpRequestRequest request) {
        HelpRequestEntity entity = findForSession(sessionId, requestId);
        if (entity.getStatus() != HelpRequestStatus.PENDING && entity.getStatus() != HelpRequestStatus.AI_LAUNCH_FAILED) {
            throw new IllegalStateException("只有待审核或启动失败的需求可以批准");
        }
        Instant now = Instant.now();
        entity.review(HelpRequestStatus.APPROVED, safeText(request == null ? null : request.reviewerNote()), now);
        helpRequestRepository.save(entity);
        auditService.logEvent(sessionId, "HELP_REQUEST_APPROVED", Map.of("requestId", requestId));
        return toResponse(entity);
    }

    public HelpRequestResponse reject(String sessionId, String requestId, ReviewHelpRequestRequest request) {
        HelpRequestEntity entity = findForSession(sessionId, requestId);
        if (entity.getStatus() != HelpRequestStatus.PENDING && entity.getStatus() != HelpRequestStatus.AI_LAUNCH_FAILED) {
            throw new IllegalStateException("只有待审核或启动失败的需求可以拒绝");
        }
        Instant now = Instant.now();
        entity.review(HelpRequestStatus.REJECTED, safeText(request == null ? null : request.reviewerNote()), now);
        helpRequestRepository.save(entity);
        auditService.logEvent(sessionId, "HELP_REQUEST_REJECTED", Map.of("requestId", requestId));
        return toResponse(entity);
    }

    public HelpRequestResponse markAiLaunched(String sessionId, String requestId, ReviewHelpRequestRequest request) {
        HelpRequestEntity entity = findForSession(sessionId, requestId);
        if (entity.getStatus() != HelpRequestStatus.APPROVED && entity.getStatus() != HelpRequestStatus.AI_LAUNCH_FAILED) {
            throw new IllegalStateException("只有已批准或启动失败的需求可以标记 AI 已启动");
        }
        entity.markAiState(HelpRequestStatus.AI_LAUNCHED, safeText(request == null ? null : request.reviewerNote()),
                Instant.now());
        helpRequestRepository.save(entity);
        auditService.logEvent(sessionId, "HELP_REQUEST_AI_LAUNCHED", Map.of("requestId", requestId));
        return toResponse(entity);
    }

    public HelpRequestResponse markAiLaunchFailed(String sessionId, String requestId, ReviewHelpRequestRequest request) {
        HelpRequestEntity entity = findForSession(sessionId, requestId);
        entity.markAiState(HelpRequestStatus.AI_LAUNCH_FAILED,
                safeText(request == null ? null : request.reviewerNote()), Instant.now());
        helpRequestRepository.save(entity);
        auditService.logEvent(sessionId, "HELP_REQUEST_AI_LAUNCH_FAILED", Map.of("requestId", requestId));
        return toResponse(entity);
    }

    private HelpRequestEntity findForSession(String sessionId, String requestId) {
        requireOnlineSession(sessionId);
        HelpRequestEntity entity = helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("需求不存在：" + requestId));
        if (!entity.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("需求不属于当前 session");
        }
        return entity;
    }

    private void requireOnlineSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        deviceRegistry.findOnlineBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("未找到在线 Agent session：" + sessionId));
    }

    private String safeText(String value) {
        return value == null ? "" : value.strip();
    }

    private HelpRequestResponse toResponse(HelpRequestEntity entity) {
        return new HelpRequestResponse(
                entity.getRequestId(),
                entity.getSessionId(),
                entity.getContent(),
                entity.getStatus(),
                entity.getReviewerNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getReviewedAt()
        );
    }
}
