package com.airh.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * list_dir 结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListDirResult {
    private String path;
    private List<FileItem> items;
    private int totalCount;
    private int blockedCount;

    public ListDirResult() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public List<FileItem> getItems() { return items; }
    public void setItems(List<FileItem> items) { this.items = items; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }
}
