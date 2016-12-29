
package com.epam.azn.model;

import java.util.List;

public class JqlResult {

    private long total;
    private List<JiraIssue> issues;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<JiraIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<JiraIssue> issues) {
        this.issues = issues;
    }
}
