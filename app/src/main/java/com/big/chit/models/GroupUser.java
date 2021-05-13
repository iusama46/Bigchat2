package com.big.chit.models;

/**
 * Created by Ussama Iftikhar on 11-May-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */
public class GroupUser {
    String id;
    String image;
    int shortID;

    public GroupUser(String id, String image, int shortID) {
        this.id = id;
        this.image = image;
        this.shortID = shortID;
    }

    public int getShortID() {
        return shortID;
    }

    public void setShortID(int shortID) {
        this.shortID = shortID;
    }

    public GroupUser(String id, String image) {
        this.id = id;
        this.image = image;
    }

    public GroupUser() {

    }

    public GroupUser(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
