package com.hodastar.photosreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhotosReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotosReviewApplication.class, args);
    }

}
