package com.hodastar.photosreview;

import com.hodastar.photosreview.mappers.ReviewMapper;
import com.hodastar.photosreview.service.DataService;
import com.hodastar.photosreview.utils.CryptUtil;
import com.hodastar.photosreview.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.hodastar.photosreview.config.Config.LOGIN_SESSION_FILE_DIR;

@SpringBootTest
class PhotosReviewApplicationTests {

    private static final Logger log =
            LoggerFactory.getLogger(PhotosReviewApplicationTests.class);

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private DataService dataService;

    @Test
    void contextLoads() throws Exception {
        f();
    }

    void a() {
        System.out.println(FileUtil.readDocumentFile(LOGIN_SESSION_FILE_DIR+"10000.session"));
    }
    void b() {
        System.out.println(CryptUtil.BCEcrypt("Aa123456"));
    }

    void c() throws Exception {
        String baseDir = System.getProperty("user.dir");
        // 目标目录
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                "d2a18223-d148-4242-b7e3-23f7fa7c283d" + File.separator +
                "img" + File.separator;
        String fileName = "d6c7fe75-97a6-4192-aa79-fc811ac3071b.JPG";
        FileUtil.deleteFile(dirStr, fileName);
    }

    void d() {

        JsonMapper jsonMapper = new JsonMapper();

        String jsonString = "{\"a\":[1,2,3],\"b\":[\"x\",\"y\"]}";
        // String jsonString = "[1,2,3]";

        JsonNode root = jsonMapper.readTree(jsonString);

        if (root.isObject()) {
            System.out.println("Map 类型");
        } else if (root.isArray()) {
            System.out.println("List 类型");
        }
    }

    void e() {
        System.out.println(
                dataService.saveProjData("d2a18223-d148-4242-b7e3-23f7fa7c283d")
        );
    }

    void f() {
        System.out.println(
                reviewMapper.getRecheckPhotos("d2a18223-d148-4242-b7e3-23f7fa7c283d")
        );
    }
}
