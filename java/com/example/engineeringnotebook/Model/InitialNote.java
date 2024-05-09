package com.example.engineeringnotebook.Model;

/**
 * Returns an Image object that can then be painted on the screen.
 * The url argument must specify an absolute <a href="#{@link}">{@link }</a>. The name
 * argument is a specifier that is relative to the url argument.
 * <p>
 * This method always returns immediately, whether or not the
 * image exists. When this applet attempts to draw the image on
 * the screen, the data will be loaded. The graphics primitives
 * that draw the image will incrementally paint on the screen.
 *
 * @param @title  an absolute URL giving the base location of the image
 * @param  @dateCreated the location of the image, relative to the url argument
 *
 * @see
 */
public class InitialNote {
    private String title;
    private String noteID;
    private String dateCreated;
    private String timeCreated;
    private String lastModifiedDate;


    public InitialNote() {
    }

    public InitialNote(String title, String dateCreated, String timeCreated, String lastModifiedDate) {
        this.title = title;
        this.dateCreated = dateCreated;
        this.timeCreated = timeCreated;
        this.lastModifiedDate = lastModifiedDate;
    }

    public InitialNote(String noteID, String title, String dateCreated, String timeCreated, String lastModifiedDate) {
        this.noteID = noteID;
        this.title = title;
        this.dateCreated = dateCreated;
        this.timeCreated = timeCreated;
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNoteID() {
        return noteID;
    }

    public void setNoteID(String noteID) {
        this.noteID = noteID;
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

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
