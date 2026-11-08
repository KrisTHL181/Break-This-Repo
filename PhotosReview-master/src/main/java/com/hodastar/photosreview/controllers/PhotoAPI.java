package com.hodastar.photosreview.controllers;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/photo")
public class PhotoAPI {
    @GetMapping("/review/{proj}/{filename}")
    public ResponseEntity<Resource> photo(
            @PathVariable String proj,
            @PathVariable String filename
    ) throws Exception {
        // 构建文件路径
        Path filePath = Path.of("data", "proj", proj, "proxy", filename);
        Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        contentType != null ? contentType : "application/octet-stream"
                ))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}
