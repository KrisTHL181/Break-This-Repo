package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewProj;
import com.hodastar.photosreview.entities.EntityReviewRecheck;
import com.hodastar.photosreview.entities.EntityReviewUsers;
import com.hodastar.photosreview.mappers.ProjMapper;
import com.hodastar.photosreview.mappers.ReviewMapper;
import com.hodastar.photosreview.mappers.UserMapper;
import com.hodastar.photosreview.utils.FileUtil;
import com.hodastar.photosreview.utils.ImageUtils;
import com.hodastar.photosreview.utils.Respond;
import com.hodastar.photosreview.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/proj")
public class ProjAPI {
    private final UserMapper userMapper;
    private final ProjMapper projMapper;
    private final ReviewMapper reviewMapper;
    private static final Logger log =
            LoggerFactory.getLogger(ProjAPI.class);
    private final Map<Integer, Long> progressAllCooldown = new ConcurrentHashMap<>();
    private ObjectMapper jsonMapper = new ObjectMapper();

    public ProjAPI(ProjMapper projMapper, UserMapper userMapper, ReviewMapper reviewMapper) {
        this.projMapper = projMapper;
        this.userMapper = userMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 通过photoid删除图片
     * @param id photoid
     * @return
     */
    private Boolean deletePhotoMethod(int id) {
        // 获取照片
        Optional<EntityReviewPhotos> photoOpt = projMapper.getPhotoById(id);
        if (photoOpt.isEmpty()) {
            return false;
        }
        // 获取文件信息
        String name = photoOpt.get().name;
        // webp版文件名
        String webpName = name + ".webp";
        // 获取工程ID
        String projId = photoOpt.get().proj;
        // 删除数据
        Boolean result = projMapper.deletePhotoById(id);
        if (!result) {
            return false;
        }
        // 文件目录
        String baseDir = System.getProperty("user.dir");
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "img" + File.separator;
        String thumbnailDirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "thumbnail" + File.separator;
        String proxyDirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "proxy" + File.separator;
        // 尝试删除文件
        try {
            FileUtil.deleteFile(dirStr, name);
            FileUtil.deleteFile(thumbnailDirStr, name);
            FileUtil.deleteFile(proxyDirStr, webpName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    // 获取工程审核进度
    @GetMapping("/get_proj_progress")
    public Respond<List<HashMap<String, Object>>> get_proj_progress(
            @RequestParam("proj") String projName,
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 获取工程
        Optional<EntityReviewProj> projOpt = projMapper.getProjByName(projName);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        if (projOpt.get().status != 1 && projOpt.get().status != 3) {
            return new Respond<>(false, "24", null);
        }

        ObjectMapper jsonMapper = new ObjectMapper();
        List<EntityReviewUsers> users = userMapper.getUserList();
        Map<String, String> uidNameMap = new HashMap<>();
        for (EntityReviewUsers user : users) {
            uidNameMap.put(String.valueOf(user.uid), user.allname);
        }

        if (projOpt.get().status == 3) {
            List<HashMap<String, Object>> data = new java.util.ArrayList<>();
            if (projOpt.get().recheck == null || projOpt.get().recheck.isBlank()) {
                return new Respond<>(true, "success", data);
            }

            List<String> recheckUsers = jsonMapper.readValue(
                    projOpt.get().recheck,
                    new tools.jackson.core.type.TypeReference<List<String>>() {}
            );
            if (recheckUsers == null || recheckUsers.isEmpty()) {
                return new Respond<>(true, "success", data);
            }

            List<EntityReviewRecheck> recheckPhotos = reviewMapper.getRecheckPhotos(projOpt.get().projId);
            for (String uid : new LinkedHashSet<>(recheckUsers)) {
                int read = 0;
                for (EntityReviewRecheck photo : recheckPhotos) {
                    if (photo.value == null || photo.value.isBlank()) {
                        continue;
                    }
                    HashMap<String, List<Object>> value = jsonMapper.readValue(
                            photo.value,
                            new tools.jackson.core.type.TypeReference<HashMap<String, List<Object>>>() {}
                    );
                    if (value.containsKey(uid) && value.get(uid) != null) {
                        read++;
                    }
                }

                HashMap<String, Object> userProgress = new HashMap<>();
                userProgress.put("uid", uid);
                userProgress.put("allname", uidNameMap.getOrDefault(uid, uid));
                userProgress.put("all", recheckPhotos.size());
                userProgress.put("read", read);
                userProgress.put("remaining", recheckPhotos.size() - read);
                data.add(userProgress);
            }
            return new Respond<>(true, "success", data);
        }

        HashMap<String, List<List<Integer>>> taskAll = jsonMapper.readValue(
                projOpt.get().task,
                new tools.jackson.core.type.TypeReference<HashMap<String, List<List<Integer>>>>() {}
        );

        // 工程ID
        String projId = projOpt.get().projId;
        // 结果列表
        List<HashMap<String, Object>> data = new java.util.ArrayList<>();

        for (Map.Entry<String, List<List<Integer>>> entry : taskAll.entrySet()) {
            String uid = entry.getKey();
            List<List<Integer>> taskList = entry.getValue();

            Set<EntityReviewPhotos> photosSet = new LinkedHashSet<>();
            for (List<Integer> range : taskList) {
                if (range == null || range.size() < 2) {
                    continue;
                }
                List<EntityReviewPhotos> photos = reviewMapper.getPhotos(projId, range.get(0), range.get(1));
                photosSet.addAll(photos);
            }

            int all = photosSet.size();
            int read = 0;
            if (projOpt.get().type == 0) {
                for (EntityReviewPhotos photo : photosSet) {
                    if (photo.value == null || photo.value.isBlank()) {
                        continue;
                    }
                    HashMap<String, List<Object>> value = jsonMapper.readValue(
                            photo.value,
                            new tools.jackson.core.type.TypeReference<HashMap<String, List<Object>>>() {}
                    );
                    if (value.containsKey(uid) && value.get(uid) != null) {
                        read++;
                    }
                }
            } else if (projOpt.get().type == 1) {
                for (EntityReviewPhotos photo : photosSet) {
                    if (photo.value == null || photo.value.isBlank()) {
                        continue;
                    }
                    List<Object> value = jsonMapper.readValue(
                            photo.value,
                            new tools.jackson.core.type.TypeReference<List<Object>>() {}
                    );
                    if (!value.isEmpty()) {
                        read++;
                    }
                }
            } else {
                return new Respond<>(false, "1", null);
            }

            HashMap<String, Object> userProgress = new HashMap<>();
            userProgress.put("uid", uid);
            userProgress.put("allname", uidNameMap.getOrDefault(uid, uid));
            userProgress.put("all", all);
            userProgress.put("read", read);
            userProgress.put("remaining", all - read);
            data.add(userProgress);
        }

        return new Respond<>(true, "success", data);
    }

    // 获取所有工程用户审核进度（管理员，5秒冷却）
    @GetMapping("/get_proj_progress_all")
    public Respond<List<HashMap<String, Object>>> get_proj_progress_all(
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        long now = System.currentTimeMillis();
        Long lastTime = progressAllCooldown.get(adminUid);
        if (lastTime != null && now - lastTime < 5000) {
            return new Respond<>(false, "28", null);
        }
        progressAllCooldown.put(adminUid, now);
        // 工程列表
        List<EntityReviewProj> projList = projMapper.getProjList().stream()
                .filter(proj -> proj.status == 1 || proj.status == 3)
                .toList();
        // 用户列表
        Map<String, String> uidNameMap = userMapper.getUserList().stream()
                .collect(java.util.stream.Collectors.toMap(
                        user -> String.valueOf(user.uid),
                        user -> user.allname
                ));
        // 结果Map
        Map<String, HashMap<String, Object>> resultMap = new HashMap<>();

        for (EntityReviewProj proj : projList) {
            if (proj.status == 3) {
                if (proj.recheck == null || proj.recheck.isBlank()) {
                    continue;
                }
                List<String> recheckUsers = jsonMapper.readValue(
                        proj.recheck,
                        new tools.jackson.core.type.TypeReference<List<String>>() {}
                );
                if (recheckUsers == null || recheckUsers.isEmpty()) {
                    continue;
                }

                List<EntityReviewRecheck> recheckPhotos = reviewMapper.getRecheckPhotos(proj.projId);
                for (String uid : new LinkedHashSet<>(recheckUsers)) {
                    int read = 0;
                    for (EntityReviewRecheck photo : recheckPhotos) {
                        if (photo.value == null || photo.value.isBlank()) {
                            continue;
                        }
                        HashMap<String, List<Object>> value = jsonMapper.readValue(
                                photo.value,
                                new tools.jackson.core.type.TypeReference<HashMap<String, List<Object>>>() {}
                        );
                        if (value.containsKey(uid) && value.get(uid) != null) {
                            read++;
                        }
                    }

                    HashMap<String, Object> userData = resultMap.getOrDefault(uid, new HashMap<>());
                    userData.put("uid", uid);
                    userData.put("allname", uidNameMap.getOrDefault(uid, uid));
                    userData.put("all", ((int) userData.getOrDefault("all", 0)) + recheckPhotos.size());
                    userData.put("read", ((int) userData.getOrDefault("read", 0)) + read);
                    resultMap.put(uid, userData);
                }
                continue;
            }
            // 任务列表
            HashMap<String, List<List<Integer>>> taskAll = jsonMapper.readValue(
                    proj.task,
                    new tools.jackson.core.type.TypeReference<HashMap<String, List<List<Integer>>>>() {}
            );
            for (Map.Entry<String, List<List<Integer>>> entry : taskAll.entrySet()) {
                String uid = entry.getKey();
                List<List<Integer>> taskList = entry.getValue();
                Set<EntityReviewPhotos> photosSet = new LinkedHashSet<>();
                for (List<Integer> range : taskList) {
                    if (range == null || range.size() < 2) {
                        continue;
                    }
                    photosSet.addAll(reviewMapper.getPhotos(proj.projId, range.get(0), range.get(1)));
                }
                int all = photosSet.size();
                int read = 0;
                if (proj.type == 0) {
                    for (EntityReviewPhotos photo : photosSet) {
                        if (photo.value == null || photo.value.isBlank()) continue;
                        HashMap<String, List<Object>> value = jsonMapper.readValue(
                                photo.value,
                                new tools.jackson.core.type.TypeReference<HashMap<String, List<Object>>>() {}
                        );
                        if (value.containsKey(uid) && value.get(uid) != null) read++;
                    }
                } else if (proj.type == 1) {
                    for (EntityReviewPhotos photo : photosSet) {
                        if (photo.value == null || photo.value.isBlank()) continue;
                        List<Object> value = jsonMapper.readValue(
                                photo.value,
                                new tools.jackson.core.type.TypeReference<List<Object>>() {}
                        );
                        if (!value.isEmpty()) read++;
                    }
                }

                HashMap<String, Object> userData = resultMap.getOrDefault(uid, new HashMap<>());
                userData.put("uid", uid);
                userData.put("allname", uidNameMap.getOrDefault(uid, uid));
                userData.put("all", ((int) userData.getOrDefault("all", 0)) + all);
                userData.put("read", ((int) userData.getOrDefault("read", 0)) + read);
                resultMap.put(uid, userData);
            }
        }

        List<HashMap<String, Object>> data = new java.util.ArrayList<>();
        for (HashMap<String, Object> item : resultMap.values()) {
            int all = (int) item.getOrDefault("all", 0);
            int read = (int) item.getOrDefault("read", 0);
            item.put("remaining", all - read);
            data.add(item);
        }
        return new Respond<>(true, "success", data);
    }

    // 获取工程列表
    @GetMapping("/get_proj_list")
    public Respond<List<HashMap<String, Object>>> get_proj_list() {
        List<EntityReviewProj> projListOrigin = projMapper.getProjList();
        List<HashMap<String, Object>> projList = projListOrigin.stream()
                .filter(proj -> proj.display == 1)
                .map(proj -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("proj_id", proj.projId);
                    map.put("proj_name", proj.name);
                    map.put("proj_type", proj.type);
                    map.put("proj_status", proj.status);
                    map.put("proj_thumbnail", proj.thumbnail);
                    map.put("proj_time", proj.time);
                    return map;
                }).toList();
        return new Respond<>(true, "Done", projList);
    }

    // 获取工程列表Admin
    @GetMapping("/get_proj_list_admin")
    public Respond<List<EntityReviewProj>> get_proj_list_admin(
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 获取工程
        List<EntityReviewProj> projList = projMapper.getProjList();
        return new Respond<>(true, "succeess", projList);
    }

    // 获取工程总量
    @GetMapping("/get_proj_count")
    public Respond<Integer> get_proj_count(
            @RequestParam("proj_id") String projId,
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        Integer count = projMapper.getProjDataCount(projId);
        return new Respond<>(true, "success", count);
    }

    // 获取工程
    @GetMapping("/get_proj")
    public Respond<EntityReviewProj> get_proj(
            @RequestParam("proj") String projId,
            @RequestParam("uid") int uid,
            @RequestParam("token") String token
    ) {
        // 检查token
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "4", null);
        }
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        return new Respond<>(true, "success", projOpt.get());
    }

    // 获取工程（管理员通过名称）
    @GetMapping("/get_proj_by_name")
    public Respond<EntityReviewProj> get_proj_by_name(
            @RequestParam("name") String name,
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        Optional<EntityReviewProj> projOpt = projMapper.getProjByName(name);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        return new Respond<>(true, "success", projOpt.get());
    }

    /**
     * 新建工程
     * param name 工程名称
     * param type 工程类型
     * param thumbnails 工程缩略图
     * return 新建结果
     */
    @PostMapping("create_proj")
    public Respond<String> create_proj(
            @RequestParam("file") MultipartFile iconFile,
            @RequestParam("body") String json
    ) throws Exception {
        // json转换
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> body = mapper.readValue(json, Map.class);

        // 检查参数
        if (
                !body.containsKey("name") ||
                !body.containsKey("type") ||
                !body.containsKey("max") ||
                !body.containsKey("adminUid") ||
                !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (
                !(body.get("name") instanceof String) ||
                !(body.get("type") instanceof Integer) ||
                !(body.get("max") instanceof Integer) ||
                !(body.get("adminUid") instanceof Integer) ||
                !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }
        if (iconFile == null || iconFile.isEmpty()) {
            return new Respond<>(false, "1", null);
        }

        String name = (String) body.get("name");
        int type = (Integer) body.get("type");
        int max = (Integer) body.get("max");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (name.isBlank()) {
            return new Respond<>(false, "1", null);
        }

        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // check name length
        if (name.length() > 255) {
            return new Respond<>(false, "6", null);
        }
        // check type
        if (type < 0 || type > 1) {
            return new Respond<>(false, "1", null);
        }
        // check max score
        if (max < 3 || max > 10) {
            return new Respond<>(false, "1", null);
        }
        // check name duplicate
        if (projMapper.isProjNameExist(name)) {
            return new Respond<>(false, "13", null);
        }

        // Generous a uuid
        String uuid = Utilities.generateUUID();
        // Projid
        String projId = Utilities.generateUUID();
        // gain file name extension
        String extension = FileUtil.getFileExtension(iconFile.getOriginalFilename());
        // check file extension
        if (extension.isEmpty() || !FileUtil.isValidImg(extension)) {
            return new Respond<>(false, "8", null);
        }

        // check file size (max 10MB)
        if (iconFile.getSize() > 10 * 1024 * 1024) {
            return new Respond<>(false, "9", null);
        }
        // Save
        String fileName = uuid + "." + extension;
        // 数据库
        Boolean result = projMapper.createProj(projId, name, type, max, fileName);
        if (!result) {
            return new Respond<>(false, "0", null);
        }

        // dir
        String baseDir = System.getProperty("user.dir");
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "icon" + File.separator;
        FileUtil.saveMultipartFile(iconFile, dirStr, fileName);

        return new Respond<>(true, "success", null);
    }

    // 删除工程
    @PostMapping("/delete_proj")
    public Respond<String> delete_proj(@RequestBody HashMap<String, Object> body) throws IOException {
        // 检查参数
        if (!body.containsKey("projId") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("projId") instanceof String) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        String projId = (String) body.get("projId");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 删除工程
        Boolean result = projMapper.deleteProj(projId);
        if (!result) {
            return new Respond<>(false, "0", null);
        }
        // dir
        String baseDir = System.getProperty("user.dir");
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator;
        FileUtil.removeDir(dirStr);

        return new Respond<>(true, "success", null);
    }

    /**
     * 修改工程
     * param id 工程ID
     * param name 工程名称
     * param thumbnail 工程缩略图
     * param status 工程状态
     * param display 工程是否显示
     * return 修改结果
     */
    @PostMapping("/update_proj")
    public Respond<String> update_proj(
            @RequestParam(value = "file", required = false) MultipartFile iconFile,
            @RequestParam("body") String json
    ) {
        // json转换
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> body = mapper.readValue(json, Map.class);

        // 检查参数
        if (
                !body.containsKey("projId") ||
                !body.containsKey("adminUid") ||
                !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (
                !(body.get("projId") instanceof String) ||
                !(body.get("adminUid") instanceof Integer) ||
                !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        String projId = (String) body.get("projId");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        String name = null;
        String thumbnail = null;
        int display = 0;
        int status = 0;
        Boolean isChangeImg = false;
        Boolean isChangeName = false;
        String baseDir = System.getProperty("user.dir");

        // 获取原工程信息
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        name = projOpt.get().name;
        String oldName = projOpt.get().name;
        thumbnail = projOpt.get().thumbnail;
        display = projOpt.get().display;
        status = projOpt.get().status;

        if (body.containsKey("name")) {
            String mName = (String) body.get("name");
            if (mName.isBlank()) {
                return new Respond<>(false, "1", null);
            }
            if (mName.length() > 255) {
                return new Respond<>(false, "6", null);
            }
            if (!mName.equals(name)) {
                if (projMapper.isProjNameExist(mName)) {
                    return new Respond<>(false, "13", null);
                }
                isChangeName = true;
            }
            name = mName;
        }
        if (iconFile != null && !iconFile.isEmpty()) {
            isChangeImg = true;

            // gain file name extension
            String extension = FileUtil.getFileExtension(iconFile.getOriginalFilename());
            // check file extension
            if (extension == null || extension.isEmpty() || !FileUtil.isValidImg(extension)) {
                return new Respond<>(false, "8", null);
            }
            // check file size (max 10MB)
            if (iconFile.getSize() > 10 * 1024 * 1024) {
                return new Respond<>(false, "9", null);
            }

            // new file name
            String uuid = Utilities.generateUUID();
            thumbnail = uuid + "." + extension;
        }
        if (body.containsKey("display")) {
            if (!(body.get("display") instanceof Integer)) {
                return new Respond<>(false, "1", null);
            }
            int mDisplay = (Integer) body.get("display");
            if (mDisplay != 0 && mDisplay != 1) {
                return new Respond<>(false, "1", null);
            }
            display = mDisplay;
        }
        if (body.containsKey("status")) {
            if (!(body.get("status") instanceof Integer)) {
                return new Respond<>(false, "1", null);
            }
            int mStatus = (Integer) body.get("status");
            if (mStatus < 0 || mStatus > 4) {
                return new Respond<>(false, "1", null);
            }
            status = mStatus;
        }

        // 数据库
        Boolean result = projMapper.updateProj(projId, name, thumbnail, status, display);
        if (!result) {
            return new Respond<>(false, "0", null);
        }

        if (isChangeImg) {
            // dir
            String dirStr = baseDir + File.separator +
                    "data" + File.separator +
                    "proj" + File.separator +
                    projId + File.separator +
                    "icon" + File.separator;
            // Save
            FileUtil.saveMultipartFile(iconFile, dirStr, thumbnail);
        }
        return new Respond<>(true, "success", null);
    }

    /**
     * 修改分发任务
     * param id 工程ID
     * param task 分发任务JSON字符串
     * return 修改结果
     */
    @PostMapping("/update_task")
    public Respond<String> update_task(
            @RequestBody HashMap<String, Object> body
    ) {
        // 检查参数
        if (!body.containsKey("projId") ||
            !body.containsKey("task") ||
            !body.containsKey("recheck") ||
            !body.containsKey("adminUid") ||
            !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("projId") instanceof String) ||
            !(body.get("task") instanceof String) ||
            !(body.get("recheck") instanceof String) ||
            !(body.get("adminUid") instanceof Integer) ||
            !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        String projId = (String) body.get("projId");
        String task = (String) body.get("task");
        String recheck = (String) body.get("recheck");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 获取原工程信息
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        // 更新数据库
        Boolean result = projMapper.updateProjTask(projId, task, recheck);
        if (!result) {
            return new Respond<>(false, "0", null);
        }

        return new Respond<>(true, "success", null);
    }

    /**
     * 图片上传
     * param file
     * param proj
     * param author
     */
    @PostMapping("upload_image")
    public Respond<String> uploadImage(
            @RequestParam("file") MultipartFile img,
            @RequestParam("body") String json
    ) throws Exception {
        // json转换
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> body = mapper.readValue(json, Map.class);

        // 检查参数
        if (
                !body.containsKey("projId") ||
                !body.containsKey("author") ||
                !body.containsKey("adminUid") ||
                !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (
                !(body.get("projId") instanceof String) ||
                !(body.get("author") instanceof String) ||
                !(body.get("adminUid") instanceof Integer) ||
                !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        if (img == null || img.isEmpty()) {
            return new Respond<>(false, "1", null);
        }

        String projId = (String) body.get("projId");
        String author = (String) body.get("author");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 检查token
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 获取原工程信息
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        // gain file name extension
        String extension = FileUtil.getFileExtension(img.getOriginalFilename());
        // check file extension
        if (extension.isEmpty() || !FileUtil.isValidImg(extension)) {
            return new Respond<>(false, "8", null);
        }
        // check file size (max 50MB)
        if (img.getSize() > 50 * 1024 * 1024) {
            return new Respond<>(false, "9", null);
        }

        String value;
        // 获取工程类型
        if (projOpt.get().type == 1) {
            value = "[]";
        } else {
            // 审片
            value = "{}";
        }

        String uuid = Utilities.generateUUID();
        String fileName = uuid + "." + extension;
        String baseDir = System.getProperty("user.dir");
        // 目标目录
        String dirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "img" + File.separator;
        // 缩略图目录
        String thumbnailDirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "thumbnail" + File.separator;
        // 代理图目录
        String proxyDirStr = baseDir + File.separator +
                "data" + File.separator +
                "proj" + File.separator +
                projId + File.separator +
                "proxy" + File.separator;

        // 保存文件
        try {
            // 保存
            FileUtil.saveMultipartFile(img, dirStr, fileName);
            // 生成缩略图
            ImageUtils.createThumbnail(dirStr, thumbnailDirStr, fileName, 300, 300);
            // 生成代理图
            ImageUtils.convertToWebp(dirStr, proxyDirStr, fileName);
            // 添加数据库
            Boolean r = projMapper.addPhoto(projId, author, fileName, value);
            if (!r) {
                throw new RuntimeException("sql error", null);
            }
            return new Respond<>(true, "success", null);
        } catch (Exception | Error e) {
            e.printStackTrace();
            // 删除已保存的文件
            FileUtil.deleteFile(dirStr, fileName);
            FileUtil.deleteFile(thumbnailDirStr, fileName);
            String webpName = fileName + ".webp";
            FileUtil.deleteFile(proxyDirStr, webpName);
            return new Respond<>(false, "0", null);
        }
    }

    @GetMapping("/get_recheck_list")
    public Respond<List<HashMap<String, Object>>> getRecheckList(
            @RequestParam("projId") String projId,
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (projMapper.getProjById(projId).isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        List<HashMap<String, Object>> recheckList = projMapper.getRecheckPhotoList(projId);
        recheckList.forEach(item -> item.put("recheck", true));
        return new Respond<>(true, "success", recheckList);
    }

    @PostMapping("/set_recheck")
    public Respond<Boolean> setRecheck(
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("photoid")
                || !body.containsKey("projId")
                || !body.containsKey("recheck")
                || !body.containsKey("adminUid")
                || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("photoid") instanceof Integer)
                || !(body.get("projId") instanceof String)
                || !(body.get("recheck") instanceof Boolean)
                || !(body.get("adminUid") instanceof Integer)
                || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        int photoId = (Integer) body.get("photoid");
        String projId = (String) body.get("projId");
        boolean recheck = (Boolean) body.get("recheck");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (projMapper.getProjById(projId).isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        Optional<EntityReviewPhotos> photoOpt = projMapper.getPhotoById(photoId);
        if (photoOpt.isEmpty() || !projId.equals(photoOpt.get().proj)) {
            return new Respond<>(false, "25", null);
        }

        Boolean updated;
        if (recheck) {
            updated = projMapper.addRecheck(photoId, projId);
        } else {
            updated = projMapper.deleteRecheck(photoId, projId);
        }

        if (!updated) {
            return new Respond<>(false, "0", null);
        }
        return new Respond<>(true, "success", recheck);
    }

    // 删除照片
    @PostMapping("delete_photo")
    public Respond<Boolean> deletePhoto(
            @RequestBody HashMap<String, Object> body
    ) throws IOException {
        if (!body.containsKey("id") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("id") instanceof Integer) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        int id = (Integer) body.get("id");
        int uid = (Integer) body.get("adminUid");
        String token = (String) body.get("adminToken");

        // 验证用户
        if (!userMapper.checkAdmin(uid, token)) {
            return new Respond<>(false, "5", null);
        }
        // 获取照片
        Optional<EntityReviewPhotos> photoOpt = projMapper.getPhotoById(id);
        if (photoOpt.isEmpty()) {
            return new Respond<>(false, "25", null);
        }

        Boolean result = deletePhotoMethod(id);
        if (!result) {
            return new Respond<>(false, "0", null);
        }

        return new Respond<>(true, "Done", null);
    }

    // 删除选中照片
    @PostMapping("delete_photos")
    public Respond<Integer> deletePhotos(
            @RequestBody HashMap<String, Object> body
    ) throws IOException {
        if (!body.containsKey("ids") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("ids") instanceof List) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        List<Integer> ids = (List<Integer>) body.get("ids");
        int uid = (Integer) body.get("adminUid");
        String token = (String) body.get("adminToken");

        // 验证用户
        if (!userMapper.checkAdmin(uid, token)) {
            return new Respond<>(false, "5", null);
        }

        // int successCount = projMapper.deletePhotoByIds(ids);
        int successCount = 0;
        for (int id : ids) {
            Boolean result = deletePhotoMethod(id);
            if (result) {
                successCount++;
            }
        }

        return new Respond<>(true, "Done", successCount);
    }
}
