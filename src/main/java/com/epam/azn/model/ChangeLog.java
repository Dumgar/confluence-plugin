package com.epam.azn.model;

import java.util.List;

public class ChangeLog {
    public ChangeLog() {
    }

    public ChangeLog(List<History> histories) {

        this.histories = histories;
    }

    List<History> histories;

    public List<History> getHistories() {
        return histories;
    }

    public void setHistories(List<History> histories) {

        this.histories = histories;
    }
}
