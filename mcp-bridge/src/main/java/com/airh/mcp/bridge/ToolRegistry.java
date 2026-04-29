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
        registerTool("list_directory", "List files and directories at a given path",
            objectSchema("path", "string", "Relative path to list"));
        registerTool("read_file", "Read file content from authorized directory",
            objectSchema("path", "string", "Relative path to file"));
        registerTool("write_file", "Write content to a file in authorized directory",
            writeSchema());
        registerTool("apply_patch", "Apply a unified diff patch to a file",
            objectSchema("path", "string", "Relative path to file"));
        registerTool("run_command", "Execute a shell command in authorized directory",
            commandSchema());
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

    public List<McpToolDefinition> getTools() { return tools; }

    public McpToolDefinition findTool(String name) {
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }
}
