package com.example.engineeringnotebook.Model;

public class Notebook {
    private String name;
    private String id;
    private String dateCreated;
    private String timeCreated;
    private String isLocked;

    public Notebook() {}

    public Notebook(String nbookName, String date, String time) {
        this.name = nbookName;
        this.dateCreated = date;
        this.timeCreated = time;
        this.isLocked = "No";
    }

    public Notebook(String nbookName, String date, String time, String isLocked) {
        this.name = nbookName;
        this.dateCreated = date;
        this.timeCreated = time;
        this.isLocked = isLocked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(String isLocked) {
        this.isLocked = isLocked;
    }
}
