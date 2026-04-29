package com.airh.relay.controller;

import com.airh.relay.device.DeviceConnection;
import com.airh.relay.device.DeviceRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceRegistry deviceRegistry;

    public DeviceController(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @GetMapping("/online")
    public List<DeviceConnection> onlineDevices() {
        return deviceRegistry.onlineDevices();
    }
}
