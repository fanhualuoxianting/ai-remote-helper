package com.airh.relay.controller;

import com.airh.protocol.dto.HealthResponse;
import com.airh.protocol.dto.NetworkAddressInfo;
import com.airh.relay.service.NetworkAddressService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class HealthController {
    private final NetworkAddressService networkAddressService;
    private final String appName;
    private final String version;

    public HealthController(NetworkAddressService networkAddressService,
                            @Value("${spring.application.name:ai-remote-helper-relay}") String appName,
                            @Value("${airh.version:${app.version:0.1.4-SNAPSHOT}}") String version) {
        this.networkAddressService = networkAddressService;
        this.appName = appName;
        this.version = version;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return healthResponse();
    }

    @GetMapping("/api/health")
    public HealthResponse apiHealth() {
        return healthResponse();
    }

    @GetMapping("/api/network/addresses")
    public List<NetworkAddressInfo> networkAddresses() {
        return networkAddressService.findLanIpv4Addresses();
    }

    private HealthResponse healthResponse() {
        return new HealthResponse(appName, version, Instant.now().toString(), "/ws", "UP");
    }
}
