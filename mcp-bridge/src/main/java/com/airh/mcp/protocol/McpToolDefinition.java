package com.airh.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class McpToolDefinition {
    private String name;
    private String description;
    private JsonNode inputSchema;

    public McpToolDefinition() {}

    public McpToolDefinition(String name, String description, JsonNode inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public JsonNode getInputSchema() { return inputSchema; }
    public void setInputSchema(JsonNode inputSchema) { this.inputSchema = inputSchema; }
}
