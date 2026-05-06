package com.airh.relay.controller;

import com.airh.protocol.dto.CreateHelpRequestRequest;
import com.airh.protocol.dto.HelpRequestResponse;
import com.airh.protocol.dto.ReviewHelpRequestRequest;
import com.airh.relay.service.HelpRequestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}/help-requests")
public class HelpRequestController {
    private final HelpRequestService helpRequestService;

    public HelpRequestController(HelpRequestService helpRequestService) {
        this.helpRequestService = helpRequestService;
    }

    @PostMapping
    public HelpRequestResponse create(@PathVariable("sessionId") String sessionId,
                                      @RequestBody CreateHelpRequestRequest request) {
        return helpRequestService.create(sessionId, request);
    }

    @GetMapping
    public List<HelpRequestResponse> list(@PathVariable("sessionId") String sessionId) {
        return helpRequestService.list(sessionId);
    }

    @PostMapping("/{requestId}/approve")
    public HelpRequestResponse approve(@PathVariable("sessionId") String sessionId, @PathVariable("requestId") String requestId,
                                       @RequestBody(required = false) ReviewHelpRequestRequest request) {
        return helpRequestService.approve(sessionId, requestId, request);
    }

    @PostMapping("/{requestId}/reject")
    public HelpRequestResponse reject(@PathVariable("sessionId") String sessionId, @PathVariable("requestId") String requestId,
                                      @RequestBody(required = false) ReviewHelpRequestRequest request) {
        return helpRequestService.reject(sessionId, requestId, request);
    }

    @PostMapping("/{requestId}/ai-launched")
    public HelpRequestResponse markAiLaunched(@PathVariable("sessionId") String sessionId, @PathVariable("requestId") String requestId,
                                              @RequestBody(required = false) ReviewHelpRequestRequest request) {
        return helpRequestService.markAiLaunched(sessionId, requestId, request);
    }

    @PostMapping("/{requestId}/ai-launch-failed")
    public HelpRequestResponse markAiLaunchFailed(@PathVariable("sessionId") String sessionId, @PathVariable("requestId") String requestId,
                                                  @RequestBody(required = false) ReviewHelpRequestRequest request) {
        return helpRequestService.markAiLaunchFailed(sessionId, requestId, request);
    }
}
