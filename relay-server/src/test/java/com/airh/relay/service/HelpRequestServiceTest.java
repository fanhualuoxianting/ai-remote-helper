package com.airh.relay.service;

import com.airh.protocol.dto.CreateHelpRequestRequest;
import com.airh.protocol.dto.ReviewHelpRequestRequest;
import com.airh.protocol.enums.HelpRequestStatus;
import com.airh.relay.device.DeviceConnection;
import com.airh.relay.device.DeviceRegistry;
import com.airh.relay.domain.HelpRequestEntity;
import com.airh.relay.repository.HelpRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelpRequestServiceTest {
    private HelpRequestRepository repository;
    private DeviceRegistry deviceRegistry;
    private AuditService auditService;
    private HelpRequestService service;

    @BeforeEach
    void setUp() {
        repository = mock(HelpRequestRepository.class);
        deviceRegistry = mock(DeviceRegistry.class);
        auditService = mock(AuditService.class);
        service = new HelpRequestService(repository, deviceRegistry, auditService);
        when(deviceRegistry.findOnlineBySessionId("session-1")).thenReturn(Optional.of(new DeviceConnection(
                "device-1", "测试设备", "stomp-1", "session-1", "123-456",
                "E:\\workspace", true, Instant.now().toString(), Instant.now().toString()
        )));
    }

    @Test
    void createsHelpRequestForOnlineSession() {
        var response = service.create("session-1", new CreateHelpRequestRequest("帮我看启动失败"));

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.status()).isEqualTo(HelpRequestStatus.PENDING);
        assertThat(response.content()).isEqualTo("帮我看启动失败");
        verify(repository).save(any(HelpRequestEntity.class));
    }

    @Test
    void rejectsBlankContent() {
        assertThatThrownBy(() -> service.create("session-1", new CreateHelpRequestRequest("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void rejectsUnknownSession() {
        assertThatThrownBy(() -> service.create("missing-session", new CreateHelpRequestRequest("需求")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未找到在线 Agent session");
    }

    @Test
    void listsBySession() {
        HelpRequestEntity entity = entity(HelpRequestStatus.PENDING);
        when(repository.findBySessionIdOrderByCreatedAtDesc("session-1")).thenReturn(List.of(entity));

        assertThat(service.list("session-1")).hasSize(1);
    }

    @Test
    void approvesPendingRequest() {
        HelpRequestEntity entity = entity(HelpRequestStatus.PENDING);
        when(repository.findById("request-1")).thenReturn(Optional.of(entity));

        var response = service.approve("session-1", "request-1", new ReviewHelpRequestRequest("可以执行"));

        assertThat(response.status()).isEqualTo(HelpRequestStatus.APPROVED);
        assertThat(response.reviewerNote()).isEqualTo("可以执行");
    }

    @Test
    void rejectsPendingRequest() {
        HelpRequestEntity entity = entity(HelpRequestStatus.PENDING);
        when(repository.findById("request-1")).thenReturn(Optional.of(entity));

        var response = service.reject("session-1", "request-1", new ReviewHelpRequestRequest("先补充信息"));

        assertThat(response.status()).isEqualTo(HelpRequestStatus.REJECTED);
        assertThat(response.reviewerNote()).isEqualTo("先补充信息");
    }

    @Test
    void rejectsRequestFromDifferentSession() {
        HelpRequestEntity entity = new HelpRequestEntity("request-1", "session-other", "需求",
                HelpRequestStatus.PENDING, null, Instant.now(), Instant.now(), null);
        when(repository.findById("request-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.approve("session-1", "request-1", new ReviewHelpRequestRequest("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于当前 session");
    }

    @Test
    void preventsDuplicateReview() {
        HelpRequestEntity entity = entity(HelpRequestStatus.REJECTED);
        when(repository.findById("request-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.approve("session-1", "request-1", new ReviewHelpRequestRequest("")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只有待审核");
    }

    @Test
    void marksAiLaunchStates() {
        HelpRequestEntity entity = entity(HelpRequestStatus.APPROVED);
        when(repository.findById("request-1")).thenReturn(Optional.of(entity));

        assertThat(service.markAiLaunched("session-1", "request-1", new ReviewHelpRequestRequest("prompt")).status())
                .isEqualTo(HelpRequestStatus.AI_LAUNCHED);
        assertThat(service.markAiLaunchFailed("session-1", "request-1", new ReviewHelpRequestRequest("失败")).status())
                .isEqualTo(HelpRequestStatus.AI_LAUNCH_FAILED);
    }

    private HelpRequestEntity entity(HelpRequestStatus status) {
        return new HelpRequestEntity("request-1", "session-1", "帮我修复问题", status,
                null, Instant.now(), Instant.now(), null);
    }
}
