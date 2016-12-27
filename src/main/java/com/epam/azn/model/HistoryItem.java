package com.epam.azn.model;

public class HistoryItem {
    String field;
    String fromString;
    String toString;

    public HistoryItem() {
    }

    public HistoryItem(String field, String fromString, String toString) {

        this.field = field;
        this.fromString = fromString;
        this.toString = toString;
    }

    public String getField() {
        return field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoryItem)) return false;

        HistoryItem that = (HistoryItem) o;

        return field.equals(that.field);
    }

    @Override
    public int hashCode() {
        return field.hashCode();
    }

    public String getFromString() {

        return fromString;
    }

    public String getToString() {
        return toString;
    }

    public void setField(String field) {

        this.field = field;
    }

    public void setFromString(String fromString) {
        this.fromString = fromString;
    }

    public void setToString(String toString) {
        this.toString = toString;
    }
}
