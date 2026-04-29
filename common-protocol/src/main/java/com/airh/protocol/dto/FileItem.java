package com.airh.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 文件/目录项信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileItem {
    private String name;
    private String path;
    private String type; // file / directory
    private long size;
    private String lastModified;
    private boolean hidden;
    private boolean sensitive;

    public FileItem() {}

    public FileItem(String name, String path, String type, long size, String lastModified, boolean hidden) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.hidden = hidden;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
}
