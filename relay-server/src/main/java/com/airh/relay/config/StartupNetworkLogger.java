package com.airh.relay.config;

import com.airh.protocol.dto.NetworkAddressInfo;
import com.airh.relay.service.NetworkAddressService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupNetworkLogger {
    private final NetworkAddressService networkAddressService;
    private final Environment environment;

    public StartupNetworkLogger(NetworkAddressService networkAddressService, Environment environment) {
        this.networkAddressService = networkAddressService;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = environment.getProperty("server.port", "8080");
        System.out.println();
        System.out.println("=== AI Remote Helper Relay Server 已启动 ===");
        System.out.println("本机访问：http://localhost:" + port);

        List<NetworkAddressInfo> addresses = networkAddressService.findLanIpv4Addresses();
        if (addresses.isEmpty()) {
            System.out.println("未检测到局域网地址。请检查网络连接。");
        } else {
            System.out.println("局域网访问候选：");
            for (NetworkAddressInfo info : addresses) {
                String marker = info.suggested() ? " ← 推荐" : "";
                System.out.println("  http://" + info.address() + ":" + port + "  (" + info.displayName() + ")" + marker);
            }
        }
        System.out.println("请让被协助者在 Agent 中输入同一局域网下的 IP。");
        System.out.println("================================================");
        System.out.println();
    }
}
