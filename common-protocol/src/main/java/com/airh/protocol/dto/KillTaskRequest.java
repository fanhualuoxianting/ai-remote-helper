package com.airh.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 终止任务请求
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KillTaskRequest {
    private String taskId;
    private String reason;

    public KillTaskRequest() {}

    public KillTaskRequest(String taskId, String reason) {
        this.taskId = taskId;
        this.reason = reason;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
