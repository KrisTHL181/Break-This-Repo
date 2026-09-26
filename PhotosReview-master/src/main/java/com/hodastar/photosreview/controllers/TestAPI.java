package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.mappers.SystemMapper;
import com.hodastar.photosreview.utils.Respond;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestAPI {
    private final SystemMapper systemMapper;

    public TestAPI(SystemMapper systemMapper) {
        this.systemMapper = systemMapper;
    }

    @GetMapping("/test2")
    public Respond<List<String>> test2() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add("Hello, World! " + i);
        }
        return new Respond<>(true, "测试成功", list);
    }

    @GetMapping("/test3")
    public Respond<String> test3() {
        return new Respond<>(true, "测试成功", systemMapper.getWebsiteName());
    }

    @PostMapping("/test4")
    public Respond<String> test4(
            @RequestParam("file") MultipartFile file,
            @RequestParam("json") String json
    ) {
        return new Respond<>(true, "测试成功", "This is a POST request");
    }

}
