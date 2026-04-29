package com.airh.mcp.transport;

import com.airh.mcp.protocol.McpProtocolHandler;
import com.airh.mcp.protocol.McpRequest;
import com.airh.mcp.protocol.McpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.time.Duration;

@RestController
@RequestMapping("/mcp")
public class SseTransportHandler {
    private final McpProtocolHandler protocolHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Sinks.Many<String> eventSink = Sinks.many().multicast().onBackpressureBuffer();

    public SseTransportHandler(McpProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sseEndpoint() {
        String endpointMessage = "{\"endpoint\":\"/mcp/messages\"}";
        return Flux.concat(
            Flux.just("event: endpoint\ndata: " + endpointMessage + "\n\n"),
            eventSink.asFlux()
        );
    }

    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public McpResponse handleMessage(@RequestBody McpRequest request) {
        McpResponse response = protocolHandler.handle(request);
        String event = "event: message\ndata: " + toJson(response) + "\n\n";
        eventSink.tryEmitNext(event);
        return response;
    }

    private String toJson(McpResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }
}
