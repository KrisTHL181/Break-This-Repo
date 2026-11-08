package com.hodastar.photosreview.entities;

public class EntityReviewProj {
    public int id;
    public String projId;
    public String name;
    public int type;
    public int max;
    public String task;
    public String recheck;
    public String thumbnail;
    public int status;
    public String time;
    public int display;

    public EntityReviewProj(int id, String projId, String name, int type, int max, String task, String recheck, String thumbnail, int status, String time, int display) {
        this.id = id;
        this.projId = projId;
        this.name = name;
        this.type = type;
        this.max = max;
        this.task = task;
        this.recheck = recheck;
        this.thumbnail = thumbnail;
        this.status = status;
        this.time = time;
        this.display = display;
    }
}
