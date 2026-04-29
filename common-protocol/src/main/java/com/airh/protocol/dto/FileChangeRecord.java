package com.airh.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 文件修改记录
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileChangeRecord {
    private String sessionId;
    private String taskId;
    private String filePath;
    private String backupPath;
    private String beforeHash;
    private String afterHash;
    private String changeType; // WRITE / PATCH
    private String createdAt;

    public FileChangeRecord() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
    public String getBeforeHash() { return beforeHash; }
    public void setBeforeHash(String beforeHash) { this.beforeHash = beforeHash; }
    public String getAfterHash() { return afterHash; }
    public void setAfterHash(String afterHash) { this.afterHash = afterHash; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
