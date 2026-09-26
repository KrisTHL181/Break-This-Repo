package com.hodastar.photosreview.entities;

public class EntityReviewUsers {
    public int uid;
    public String password;
    public long login_time;
    public String allname;
    public int status;

    public EntityReviewUsers(int uid, String password, long login_time, String allname, int status) {
        this.uid = uid;
        this.password = password;
        this.login_time = login_time;
        this.allname = allname;
        this.status = status;
    }
}
