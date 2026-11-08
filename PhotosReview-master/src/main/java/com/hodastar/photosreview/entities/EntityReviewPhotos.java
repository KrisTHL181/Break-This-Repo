package com.hodastar.photosreview.entities;

public class EntityReviewPhotos {
    public int id;
    public String name;
    public String proj;
    public String author;
    public String value;

    public EntityReviewPhotos(int id, String name, String proj, String author, String value) {
        this.id = id;
        this.name = name;
        this.proj = proj;
        this.author = author;
        this.value = value;
    }

    @Override
    public String toString() {
        return "EntityReviewPhotos{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", proj='" + proj + '\'' +
                ", author='" + author + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
