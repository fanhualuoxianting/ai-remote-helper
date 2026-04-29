package com.airh.mcp.protocol;

import com.airh.mcp.bridge.TaskBridge;
import com.airh.mcp.bridge.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpProtocolHandlerTest {
    private McpProtocolHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ToolRegistry toolRegistry = new ToolRegistry();
        TaskBridge taskBridge = new TaskBridge();
        handler = new McpProtocolHandler(toolRegistry, taskBridge);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testInitialize() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", "2024-11-05");
        McpRequest request = new McpRequest("1", "initialize", params);
        McpResponse response = handler.handle(request);
        assertNotNull(response);
        assertEquals("1", response.getId());
        assertFalse(response.isError());
        assertNotNull(response.getResult());
        assertTrue(response.getResult().has("serverInfo"));
        assertTrue(response.getResult().has("capabilities"));
    }

    @Test
    void testToolsList() {
        McpRequest request = new McpRequest("2", "tools/list", null);
        McpResponse response = handler.handle(request);
        assertNotNull(response);
        assertFalse(response.isError());
        JsonNode tools = response.getResult().get("tools");
        assertNotNull(tools);
        assertTrue(tools.isArray());
        assertEquals(5, tools.size());
        assertEquals("list_directory", tools.get(0).get("name").asText());
        assertEquals("read_file", tools.get(1).get("name").asText());
        assertEquals("write_file", tools.get(2).get("name").asText());
        assertEquals("apply_patch", tools.get(3).get("name").asText());
        assertEquals("run_command", tools.get(4).get("name").asText());
    }

    @Test
    void testToolsCall() {
        ObjectNode args = objectMapper.createObjectNode();
        args.put("path", ".");
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "list_directory");
        params.set("arguments", args);
        McpRequest request = new McpRequest("3", "tools/call", params);
        McpResponse response = handler.handle(request);
        assertNotNull(response);
        assertFalse(response.isError());
        assertEquals("PENDING", response.getResult().get("status").asText());
        assertEquals("LIST_DIR", response.getResult().get("taskType").asText());
    }

    @Test
    void testUnknownTool() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "unknown_tool");
        McpRequest request = new McpRequest("4", "tools/call", params);
        McpResponse response = handler.handle(request);
        assertNotNull(response);
        assertTrue(response.isError());
        assertEquals(-32602, response.getError().getCode());
    }

    @Test
    void testUnknownMethod() {
        McpRequest request = new McpRequest("5", "unknown/method", null);
        McpResponse response = handler.handle(request);
        assertNotNull(response);
        assertTrue(response.isError());
        assertEquals(-32601, response.getError().getCode());
    }

    @Test
    void testPing() {
        McpRequest request = new McpRequest("6", "ping", null);
        McpResponse response = handler.handle(request);
        assertNotNull(response);
        assertFalse(response.isError());
    }
}
