package com.epam.azn.model;

public class IssueWithHistory {
    private String key;
    private ChangeLog changelog;

    public IssueWithHistory() {
    }

    public IssueWithHistory(String key, ChangeLog changelog) {

        this.key = key;
        this.changelog = changelog;
    }

    public String getKey() {
        return key;
    }

    public ChangeLog getChangelog() {
        return changelog;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public void setChangelog(ChangeLog changelog) {
        this.changelog = changelog;
    }
}
