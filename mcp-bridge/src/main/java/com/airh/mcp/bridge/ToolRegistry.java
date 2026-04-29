package com.airh.mcp.bridge;

import com.airh.mcp.protocol.McpToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ToolRegistry {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<McpToolDefinition> tools = new ArrayList<>();

    public ToolRegistry() {
        // 设备和会话管理
        registerTool("remote_list_devices", "List all online remote devices", emptySchema());
        registerTool("remote_connect_session", "Connect to a remote session by session code",
            objectSchema("sessionCode", "string", "Session code to connect"));

        // 文件操作
        registerTool("remote_list_dir", "List files and directories on remote agent",
            objectSchema("path", "string", "Relative path to list"));
        registerTool("remote_read_file", "Read file content from remote agent authorized directory",
            objectSchema("path", "string", "Relative path to file"));
        registerTool("remote_write_file", "Write content to a file on remote agent",
            writeSchema());
        registerTool("remote_apply_patch", "Apply a unified diff patch to a file on remote agent",
            patchSchema());

        // 命令执行
        registerTool("remote_run_command", "Execute a shell command on remote agent",
            commandSchema());
        registerTool("remote_get_task_logs", "Get logs for a specific task",
            objectSchema("taskId", "string", "Task ID to get logs for"));
        registerTool("remote_kill_task", "Kill a running task on remote agent",
            objectSchema("taskId", "string", "Task ID to kill"));

        // 报告
        registerTool("remote_generate_report", "Generate session report",
            objectSchema("sessionId", "string", "Session ID to generate report for"));
    }

    private void registerTool(String name, String description, JsonNode schema) {
        tools.add(new McpToolDefinition(name, description, schema));
    }

    private JsonNode objectSchema(String propName, String propType, String propDesc) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode prop = objectMapper.createObjectNode();
        prop.put("type", propType);
        prop.put("description", propDesc);
        props.set(propName, prop);
        schema.set("properties", props);
        schema.putArray("required").add(propName);
        return schema;
    }

    private JsonNode writeSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode path = objectMapper.createObjectNode();
        path.put("type", "string");
        path.put("description", "Relative path to file");
        props.set("path", path);
        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", "string");
        content.put("description", "Content to write");
        props.set("content", content);
        schema.set("properties", props);
        schema.putArray("required").add("path").add("content");
        return schema;
    }

    private JsonNode commandSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.put("type", "string");
        cmd.put("description", "Command to execute");
        props.set("command", cmd);
        ObjectNode cwd = objectMapper.createObjectNode();
        cwd.put("type", "string");
        cwd.put("description", "Working directory (relative)");
        props.set("cwd", cwd);
        ObjectNode timeout = objectMapper.createObjectNode();
        timeout.put("type", "integer");
        timeout.put("description", "Timeout in seconds");
        props.set("timeoutSeconds", timeout);
        schema.set("properties", props);
        schema.putArray("required").add("command");
        return schema;
    }

    private JsonNode patchSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode path = objectMapper.createObjectNode();
        path.put("type", "string");
        path.put("description", "Relative path to file");
        props.set("path", path);
        ObjectNode patch = objectMapper.createObjectNode();
        patch.put("type", "string");
        patch.put("description", "Unified diff patch content");
        props.set("patch", patch);
        schema.set("properties", props);
        schema.putArray("required").add("path").add("patch");
        return schema;
    }

    private JsonNode emptySchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", objectMapper.createObjectNode());
        return schema;
    }

    public List<McpToolDefinition> getTools() { return tools; }

    public McpToolDefinition findTool(String name) {
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }
}
