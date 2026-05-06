package com.airh.protocol.dto;

public record NetworkAddressInfo(
        String interfaceName,
        String displayName,
        String address,
        boolean suggested,
        String reason
) {
}
