package com.airh.mcp.protocol;

import com.airh.mcp.bridge.TaskBridge;
import com.airh.mcp.bridge.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class McpProtocolHandler {
    private final ToolRegistry toolRegistry;
    private final TaskBridge taskBridge;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpProtocolHandler(ToolRegistry toolRegistry, TaskBridge taskBridge) {
        this.toolRegistry = toolRegistry;
        this.taskBridge = taskBridge;
    }

    public McpResponse handle(McpRequest request) {
        if (request.getMethod() == null) {
            return new McpResponse(request.getId(), -32600, "Invalid Request: missing method");
        }
        return switch (request.getMethod()) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            case "ping" -> handlePing(request);
            default -> new McpResponse(request.getId(), -32601,
                "Method not found: " + request.getMethod());
        };
    }

    private McpResponse handleInitialize(McpRequest request) {
        ObjectNode result = objectMapper.createObjectNode();
        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "ai-remote-helper-mcp-bridge");
        serverInfo.put("version", "0.1.0");
        result.set("serverInfo", serverInfo);
        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode tools = objectMapper.createObjectNode();
        tools.put("listChanged", false);
        capabilities.set("tools", tools);
        result.set("capabilities", capabilities);
        result.put("protocolVersion", "2024-11-05");
        return new McpResponse(request.getId(), result);
    }

    private McpResponse handleToolsList(McpRequest request) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode toolsArray = objectMapper.createArrayNode();
        for (var tool : toolRegistry.getTools()) {
            ObjectNode toolNode = objectMapper.createObjectNode();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema());
            toolsArray.add(toolNode);
        }
        result.set("tools", toolsArray);
        return new McpResponse(request.getId(), result);
    }

    private McpResponse handleToolsCall(McpRequest request) {
        JsonNode params = request.getParams();
        if (params == null || !params.has("name")) {
            return new McpResponse(request.getId(), -32602, "Invalid params: missing tool name");
        }
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        String taskType = taskBridge.mapToolToTaskType(toolName);
        if (taskType == null) {
            return new McpResponse(request.getId(), -32602, "Unknown tool: " + toolName);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("taskId", java.util.UUID.randomUUID().toString());
        result.put("taskType", taskType);
        result.put("status", "PENDING");
        result.put("message", "Task created and queued for execution via relay server");
        if (arguments != null) {
            result.set("arguments", arguments);
        }
        return new McpResponse(request.getId(), result);
    }

    private McpResponse handlePing(McpRequest request) {
        ObjectNode result = objectMapper.createObjectNode();
        return new McpResponse(request.getId(), result);
    }
}
