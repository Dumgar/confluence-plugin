/*
 * Canadian Tire Corporation, Ltd. Do not reproduce without permission in writing.
 * Copyright (c) 2016 Canadian Tire Corporation, Ltd. All rights reserved.
 */

package com.epam.azn;

import java.util.HashMap;

public class JiraIssue {
    private String id;
    private String self;
    private String key;
    private HashMap<String, Object> fields;

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
