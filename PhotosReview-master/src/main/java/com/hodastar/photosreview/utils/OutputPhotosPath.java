package com.hodastar.photosreview.utils;

import org.springframework.security.core.parameters.P;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputPhotosPath {
    public static Path imgPath(String proj, String name) {
        // 目标目录
        String baseDir = System.getProperty("user.dir");
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                proj + File.separator +
                "img" + File.separator;
        Path path = Paths.get(dirStr);

        return path.resolve(name);
    }

    public static Path projPath(String proj) {
        // 目标目录
        String baseDir = System.getProperty("user.dir");
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                proj + File.separator;

        return Paths.get(dirStr);
    }
}
