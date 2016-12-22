/*
 * Canadian Tire Corporation, Ltd. Do not reproduce without permission in writing.
 * Copyright (c) 2016 Canadian Tire Corporation, Ltd. All rights reserved.
 */

package com.epam.azn;

import java.util.List;

public class JqlResult {

    private long total;
    private List<String> issues;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }
}
