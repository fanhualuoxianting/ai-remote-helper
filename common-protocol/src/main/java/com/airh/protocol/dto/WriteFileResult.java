package com.airh.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * write_file 结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WriteFileResult {
    private String path;
    private long size;
    private String backupPath;
    private String beforeHash;
    private String afterHash;
    private String message;

    public WriteFileResult() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
    public String getBeforeHash() { return beforeHash; }
    public void setBeforeHash(String beforeHash) { this.beforeHash = beforeHash; }
    public String getAfterHash() { return afterHash; }
    public void setAfterHash(String afterHash) { this.afterHash = afterHash; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
