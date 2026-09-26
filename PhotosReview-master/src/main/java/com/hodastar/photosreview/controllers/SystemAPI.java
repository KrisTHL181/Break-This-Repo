package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.config.Config;
import com.hodastar.photosreview.mappers.SystemMapper;
import com.hodastar.photosreview.mappers.UserMapper;
import com.hodastar.photosreview.utils.FileUtil;
import com.hodastar.photosreview.utils.Respond;
import com.hodastar.photosreview.utils.Utilities;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemAPI {

    private final SystemMapper systemMapper;
    private final Config config;
    private final UserMapper userMapper;

    public SystemAPI(SystemMapper systemMapper, Config config, UserMapper userMapper) {
        this.systemMapper = systemMapper;
        this.config = config;
        this.userMapper = userMapper;
    }

    @RequestMapping("/get_website_info")
    public Respond<Map<String, Object>> getWebsiteInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("website_name", systemMapper.getWebsiteName());
        data.put("website_icon", systemMapper.getWebsiteIcon());
        return new Respond<>(true, "true", data);
    }

    @PostMapping("/update_website_info")
    public Respond<String> updateWebsiteInfo(
            @RequestParam(value = "file", required = false) MultipartFile iconFile,
            @RequestParam("body") String json
    ) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> body = mapper.readValue(json, Map.class);

        if (!body.containsKey("adminUid") || !body.containsKey("adminToken") || !body.containsKey("websiteName")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("adminUid") instanceof Integer) ||
                !(body.get("adminToken") instanceof String) ||
                !(body.get("websiteName") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");
        String websiteName = ((String) body.get("websiteName")).trim();

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (websiteName.isBlank()) {
            return new Respond<>(false, "1", null);
        }

        if (iconFile == null || iconFile.isEmpty()) {
            boolean resultName = systemMapper.updateWebsiteName(websiteName);
            if (!resultName) {
                return new Respond<>(false, "0", null);
            }
            return new Respond<>(true, "success", null);
        } else {
            String extension = FileUtil.getFileExtension(iconFile.getOriginalFilename());
            if (extension.isEmpty() || !FileUtil.isValidImg(extension)) {
                return new Respond<>(false, "8", null);
            }
            if (iconFile.getSize() > 10 * 1024 * 1024) {
                return new Respond<>(false, "9", null);
            }

            String fileName = Utilities.generateUUID() + "." + extension;
            String dirStr = System.getProperty("user.dir") + File.separator + "data" + File.separator + "icon" + File.separator;
            FileUtil.saveMultipartFile(iconFile, dirStr, fileName);
            String iconPath = "data/icon/" + fileName;
            boolean resultIcon = systemMapper.updateWebsiteIcon(iconPath);
            boolean resultName = systemMapper.updateWebsiteName(websiteName);
            if (!resultName || !resultIcon) {
                return new Respond<>(false, "0", null);
            }
            return new Respond<>(true, "success", null);
        }
    }
}
