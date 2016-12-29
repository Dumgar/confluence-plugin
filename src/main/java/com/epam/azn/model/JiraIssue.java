package com.epam.azn.model;

import java.util.HashMap;

public class JiraIssue {
    private String id;
    private String self;
    private String key;
    private HashMap<String, Object> fields;
    private ChangeLog changelog;

    public ChangeLog getChangelog() {
        return changelog;
    }

    public void setChangelog(ChangeLog changelog) {

        this.changelog = changelog;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(final String self) {
        this.self = self;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public HashMap<String, Object> getFields() {
        return fields;
    }

    public void setFields(final HashMap<String, Object> fields) {
        this.fields = fields;
    }
}
