package com.hodastar.photosreview.entities;

public class EntityReviewRecheck {
    public int photoid;
    public String proj;
    public String value;
    public double finalScore;

    public EntityReviewRecheck(int photoid, String proj, String value, double finalScore) {
        this.photoid = photoid;
        this.proj = proj;
        this.value = value;
        this.finalScore = finalScore;
    }

    @Override
    public String toString() {
        return "EntityReviewRecheck{" +
                "photoid=" + photoid +
                ", proj='" + proj + '\'' +
                ", value='" + value + '\'' +
                ", finalScore='" + finalScore + '\'' +
                '}';
    }
}
