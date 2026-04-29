package com.airh.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * apply_patch 结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyPatchResult {
    private String path;
    private boolean success;
    private String backupPath;
    private String beforeHash;
    private String afterHash;
    private String message;
    private int hunksApplied;

    public ApplyPatchResult() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
    public String getBeforeHash() { return beforeHash; }
    public void setBeforeHash(String beforeHash) { this.beforeHash = beforeHash; }
    public String getAfterHash() { return afterHash; }
    public void setAfterHash(String afterHash) { this.afterHash = afterHash; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getHunksApplied() { return hunksApplied; }
    public void setHunksApplied(int hunksApplied) { this.hunksApplied = hunksApplied; }
}
