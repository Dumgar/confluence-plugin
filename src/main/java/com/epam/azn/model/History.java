package com.epam.azn.model;

import java.util.List;

public class History {
    String created;

    List<HistoryItem> items;

    public History() {
    }

    public History(String created, List<HistoryItem> items) {

        this.created = created;
        this.items = items;
    }

    public String getCreated() {
        return created;
    }

    public List<HistoryItem> getItems() {
        return items;
    }

    public void setCreated(String created) {

        this.created = created;
    }

    public void setItems(List<HistoryItem> items) {
        this.items = items;
    }
}
