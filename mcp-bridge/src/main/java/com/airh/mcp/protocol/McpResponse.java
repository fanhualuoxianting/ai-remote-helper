package com.airh.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpResponse {
    private String jsonrpc = "2.0";
    private String id;
    private JsonNode result;
    private McpError error;

    public McpResponse() {}

    public McpResponse(String id, JsonNode result) {
        this.id = id;
        this.result = result;
    }

    public McpResponse(String id, int code, String message) {
        this.id = id;
        this.error = new McpError(code, message);
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public JsonNode getResult() { return result; }
    public void setResult(JsonNode result) { this.result = result; }
    public McpError getError() { return error; }
    public void setError(McpError error) { this.error = error; }

    public boolean isError() { return error != null; }

    public static class McpError {
        private int code;
        private String message;

        public McpError() {}

        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
