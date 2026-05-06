package com.airh.relay.service;

import com.airh.protocol.dto.NetworkAddressInfo;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

@Service
public class NetworkAddressService {

    public List<NetworkAddressInfo> findLanIpv4Addresses() {
        List<NetworkAddressInfo> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return List.of();
        }
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            try {
                if (!isUsableInterface(ni)) continue;
            } catch (SocketException e) {
                continue;
            }
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                if (!(ia.getAddress() instanceof Inet4Address ipv4)) continue;
                String addr = ipv4.getHostAddress();
                if (!isPrivateLanAddress(addr)) continue;
                boolean suggested = isPreferredPhysicalInterface(ni);
                addresses.add(new NetworkAddressInfo(
                        ni.getName(), ni.getDisplayName(), addr, suggested,
                        suggested ? "Wi-Fi/以太网局域网地址，推荐给 Agent 使用" : "可用局域网 IPv4 地址"
                ));
            }
        }
        return addresses.stream()
                .sorted(Comparator.comparing(NetworkAddressInfo::suggested).reversed()
                        .thenComparing(NetworkAddressInfo::interfaceName)
                        .thenComparing(NetworkAddressInfo::address))
                .toList();
    }

    private boolean isUsableInterface(NetworkInterface ni) throws SocketException {
        return ni.isUp() && !ni.isLoopback() && !ni.isVirtual() && !looksLikeVirtualAdapter(ni);
    }

    private boolean looksLikeVirtualAdapter(NetworkInterface ni) {
        String text = (ni.getName() + " " + ni.getDisplayName()).toLowerCase(Locale.ROOT);
        return text.contains("docker") || text.contains("virtualbox") || text.contains("vbox")
                || text.contains("vmware") || text.contains("hyper-v") || text.contains("wsl")
                || text.contains("loopback") || text.contains("vethernet") || text.contains("tunnel")
                || text.contains("tap") || text.contains("npcap");
    }

    private boolean isPreferredPhysicalInterface(NetworkInterface ni) {
        String text = (ni.getName() + " " + ni.getDisplayName()).toLowerCase(Locale.ROOT);
        return text.contains("wi-fi") || text.contains("wifi") || text.contains("wlan")
                || text.contains("wireless") || text.contains("ethernet") || text.contains("以太")
                || text.contains("无线");
    }

    private boolean isPrivateLanAddress(String address) {
        if (address.startsWith("127.")) return false;
        String[] parts = address.split("\\.");
        if (parts.length != 4) return false;
        int first = parseOctet(parts[0]);
        int second = parseOctet(parts[1]);
        return first == 10 || (first == 172 && second >= 16 && second <= 31) || (first == 192 && second == 168);
    }

    private int parseOctet(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return -1; }
    }
}
