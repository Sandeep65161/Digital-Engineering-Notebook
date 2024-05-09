package com.example.engineeringnotebook.Model;

public class DateStamp {
    private String lastModifiedTime;
    private String lastModifiedDate;

    public DateStamp() {
    }

    public DateStamp(String lastModifiedTime, String lastModifiedDate) {
        this.lastModifiedTime = lastModifiedTime;
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}

