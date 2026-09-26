package com.hodastar.photosreview.entities;

public class EntityReviewResult {
    public int id;
    public String proj;
    public String value;
    public String time;

    public EntityReviewResult(int id, String proj, String value, String time) {
        this.id = id;
        this.proj = proj;
        this.value = value;
        this.time = time;
    }

    @Override
    public String toString() {
        return "EntityReviewResult{" +
                "id=" + id +
                ", proj='" + proj + '\'' +
                ", value='" + value + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
