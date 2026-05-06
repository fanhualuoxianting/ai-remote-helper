package com.airh.relay.controller;

import com.airh.protocol.dto.FileChangeRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FileChangeController {
    private final Map<String, List<FileChangeRecord>> sessionChanges = new ConcurrentHashMap<>();

    @PostMapping("/sessions/{sessionId}/file-changes")
    public ResponseEntity<FileChangeRecord> recordFileChange(
            @PathVariable("sessionId") String sessionId,
            @RequestBody FileChangeRecord record) {
        record.setSessionId(sessionId);
        record.setCreatedAt(Instant.now().toString());
        sessionChanges.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>())).add(record);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/sessions/{sessionId}/file-changes")
    public ResponseEntity<List<FileChangeRecord>> getFileChanges(@PathVariable("sessionId") String sessionId) {
        List<FileChangeRecord> changes = sessionChanges.getOrDefault(sessionId, List.of());
        return ResponseEntity.ok(changes);
    }
}
