package com.example.engineeringnotebook.Model;

public class SharedNbStructure {
    private String name;
    private String shareNbID;
    private String recipientEmail;
    private String ownerID;
    private String dateCreated;
    private String dateShared;
    private String permissions;
    private String uniqueSharedNBID;
    private String revoked;

    public SharedNbStructure() {
    }

    public SharedNbStructure(String sharedNbName, String email, String uniqueSharedNBID ,String shareNbID, String ownerID, String date, String dateShared, String permission) {
        this.name = sharedNbName;
        this.dateCreated = date;
        this.dateShared = dateShared;
        this.recipientEmail = email;
        this.ownerID = ownerID;
        this.permissions = permission;
        this.shareNbID = shareNbID;
        this.uniqueSharedNBID = uniqueSharedNBID;
        this.revoked = "No";
    }

    public SharedNbStructure(String sharedNbName, String email, String uniqueSharedNBID ,String shareNbID, String ownerID, String date, String dateShared, String permission, String revoked) {
        this.name = sharedNbName;
        this.dateCreated = date;
        this.dateShared = dateShared;
        this.recipientEmail = email;
        this.ownerID = ownerID;
        this.permissions = permission;
        this.shareNbID = shareNbID;
        this.uniqueSharedNBID = uniqueSharedNBID;
        this.revoked = revoked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShareNbID() {
        return shareNbID;
    }

    public void setShareNbID(String shareNbID) {
        this.shareNbID = shareNbID;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public String getUniqueSharedNBID() {
        return uniqueSharedNBID;
    }

    public void setUniqueSharedNBID(String uniqueSharedNBID) {
        this.uniqueSharedNBID = uniqueSharedNBID;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDateShared() {
        return dateShared;
    }

    public void setDateShared(String dateShared) {
        this.dateShared = dateShared;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getRevoked() {
        return revoked;
    }

    public void setRevoked(String revoked) {
        this.revoked = revoked;
    }
}
