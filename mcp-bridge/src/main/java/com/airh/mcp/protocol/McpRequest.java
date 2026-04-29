package com.airh.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpRequest {
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private JsonNode params;

    public McpRequest() {}

    public McpRequest(String id, String method, JsonNode params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public JsonNode getParams() { return params; }
    public void setParams(JsonNode params) { this.params = params; }
}
