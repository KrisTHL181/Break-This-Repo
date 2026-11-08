package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewProj;
import com.hodastar.photosreview.entities.EntityReviewRecheck;
import com.hodastar.photosreview.mappers.ReviewMapper;
import com.hodastar.photosreview.mappers.ProjMapper;
import com.hodastar.photosreview.mappers.UserMapper;
import com.hodastar.photosreview.utils.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

@RestController
@RequestMapping("/api/review")
public class ReviewAPI {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProjMapper projMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    private static final Logger log =
            LoggerFactory.getLogger(ReviewAPI.class);
    private ObjectMapper jsonMapper = new ObjectMapper();

    // 检查int是否存在多个集合中的其中一个
    private boolean checkIntInCollections(int value, List<List<Integer>> collections) {
        return collections.stream().anyMatch(x -> x.contains(value));
    }

    // 添加图片进入数组
    private HashMap<String, Object> addPhotoToList(int photoid, String name, String type, int score, String note) {
        // 定义数组
        HashMap<String, Object> photoData = new HashMap<>();
        // 判断
        if (type.equals("unread")) {
            photoData.put("photoid", photoid);
            photoData.put("name", name);
            photoData.put("status", false);
        } else if (type.equals("read")) {
            photoData.put("photoid", photoid);
            photoData.put("name", name);
            photoData.put("score", score);
            photoData.put("note", note);
            photoData.put("status", true);
        }
        return photoData;
    }

    // 审片模式数据修改
    private String updateReviewData(
            int uid,
            String originalValue,
            int score,
            String note
    ) {
        // 解析
        HashMap<String, List<Object>> value =
                jsonMapper.readValue(
                        originalValue,
                        new TypeReference<HashMap<String, List<Object>>>() {}
                );

        // 更新评分数据
        List<Object> newScoreData = new ArrayList<>();
        newScoreData.add(score);
        newScoreData.add(note);
        value.put(String.valueOf(uid), newScoreData);
        // 转换为json字符串
        return jsonMapper.writeValueAsString(value);
    }

    // 筛片模式数据修改
    private String updateScreenData(
            int uid,
            int score,
            String note
    ) {
        // 定义新的value
        List<Object> newValue = new ArrayList<>();
        newValue.add(String.valueOf(uid));
        newValue.add(score);
        newValue.add(note);
        // 转换为json字符串
        return jsonMapper.writeValueAsString(newValue);
    }

    // 单次提交评分
    private Boolean submitSingle(
            int uid,
            Optional<EntityReviewProj> projOpt,
            Optional<EntityReviewPhotos> photoOpt,
            int score, String note
    ) {
        if (photoOpt.isEmpty()) {
            return false;
        }
        // 判断工程类型
        if (projOpt.get().type == 0) {
            String newValueStr = updateReviewData(uid, photoOpt.get().value, score, note);

            // 更新数据库
            Boolean updateResult = reviewMapper.updatePhotoValue(photoOpt.get().id, newValueStr);
            if (!updateResult) {
                return false;
            }
        } else if (projOpt.get().type == 1) {
            String newValueStr = updateScreenData(uid, score, note);

            // 更新数据库
            Boolean updateResult = reviewMapper.updatePhotoValue(photoOpt.get().id, newValueStr);
            if (!updateResult) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    // 单次提交复审评分
    private Boolean submitRecheckSingle(
            int uid,
            Optional<EntityReviewRecheck> photoOpt,
            int score, String note
    ) {
        if (photoOpt.isEmpty()) {
            return false;
        }
        String newValueStr = updateReviewData(uid, photoOpt.get().value, score, note);

        // 更新数据库
        Boolean updateResult = reviewMapper.updateRecheckValue(photoOpt.get().photoid, newValueStr);
        if (!updateResult) {
            return false;
        }
        return true;
    }

    // 单次删除某uid评分
    private HashMap<String, Object> deleteScoreSingle(
            String targetUid,
            int photoid,
            String originalValue,
            int projType
    ) {
        HashMap<String, Object> result = new HashMap<>();
        ObjectMapper jsonMapper = new ObjectMapper();

        if (originalValue.isEmpty()) {
            result.put("status", false);
            result.put("message", "0");
            return result;
        }

        if (projType == 0) {
            // 审片
            HashMap<String, List<Object>> value =
                    jsonMapper.readValue(
                            originalValue,
                            new TypeReference<HashMap<String, List<Object>>>() {}
                    );
            // 是否含有此用户评分
            if (!value.containsKey(targetUid)) {
                result.put("status", false);
                result.put("message", "20");
                return result;
            }
            // 新的value
            HashMap<String, List<Object>> newValue = new HashMap<>(value);
            // 删除指定uid的评分数据
            newValue.remove(targetUid);
            // 转换为json字符串
            String newValueStr = jsonMapper.writeValueAsString(newValue);
            // 更新数据库
            Boolean updateResult = reviewMapper.updatePhotoValue(photoid, newValueStr);
            if (!updateResult) {
                result.put("status", false);
                result.put("message", "0");
                return result;
            } else {
                result.put("status", true);
                return result;
            }
        } else if (projType == 1) {
            // 筛片
            List<Object> value = jsonMapper.readValue(
                    originalValue,
                    new TypeReference<List<Object>>() {}
            );
            if (value.isEmpty()) {
                result.put("status", false);
                result.put("message", "0");
                return result;
            }
            // 是否是此用户评分
            if (!Objects.equals(String.valueOf(value.get(0)), targetUid)) {
                result.put("status", false);
                result.put("message", "20");
                return result;
            }
            // 筛片
            String newValueStr = "[]";
            // 更新数据库
            Boolean updateResult = reviewMapper.updatePhotoValue(photoid, newValueStr);
            if (!updateResult) {
                result.put("status", false);
                result.put("message", "0");
                return result;
            } else {
                result.put("status", true);
                return result;
            }

        } else {
            result.put("status", true);
            return result;
        }
    }

    // 获取照片列表
    @GetMapping("/fetch_photo_list")
    public Respond<HashMap<String, Object>> fetchPhotoList(
            @RequestParam("uid") int uid,
            @RequestParam("token") String token,
            @RequestParam("proj") String projId
    ) {
        // 验证用户
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "4", null);
        }

        // 获取项目
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 检查工程状态
        if (projOpt.get().status != 1) {
            return new Respond<>(false, "24", null);
        }

        // 获取任务内容
        HashMap<String, List<List<Integer>>> taskAll =
                jsonMapper.readValue(
                        projOpt.get().task,
                        new TypeReference<HashMap<String, List<List<Integer>>>>() {}
                );
        List<List<Integer>> taskList = taskAll.get(String.valueOf(uid));

        if (taskList == null || taskList.isEmpty()) {
            return new Respond<>(false, "27", null);
        }

        Set<EntityReviewPhotos> photosSet = new LinkedHashSet<>();
        // 循环每个任务
        for (List<Integer> list : taskList) {
            // 获取照片列表
            List<EntityReviewPhotos> photos = reviewMapper.getPhotos(projId, list.get(0), list.get(1));
            // 添加集合
            photosSet.addAll(photos);
        }
        // 所有图片
        List<EntityReviewPhotos> photosList = new ArrayList<>(photosSet);
        // unread数组集合
        List<HashMap<String, Object>> unreadList = new ArrayList<>();
        // read数组集合
        List<HashMap<String, Object>> readList = new ArrayList<>();

        // 审片模式
        if (projOpt.get().type == 0) {
            // 遍历图片列表
            for (EntityReviewPhotos photo : photosList) {
                String valueStr = photo.value;

                // 如果value字段为空或null
                if (valueStr == null) {
                    unreadList.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                    continue;
                }

                // 读取value字段
                HashMap<String, List<Object>> value =
                        jsonMapper.readValue(
                        valueStr,
                        new TypeReference<HashMap<String, List<Object>>>() {}
                    );

                if (!value.containsKey(String.valueOf(uid)) || value.get(String.valueOf(uid)) == null) {
                    // 如果数据中没有该uid的数据
                    unreadList.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                } else {
                    // 添加
                    List<Object> reviewData = value.get(String.valueOf(uid));
                    int score = (int) reviewData.get(0);
                    String note = (String) reviewData.get(1);
                    readList.add(addPhotoToList(photo.id, photo.name, "read", score, note));
                }
            }
        }
        // 筛片模式
        if (projOpt.get().type == 1) {
            // 遍历图片列表
            for (EntityReviewPhotos photo : photosList) {
                String valueStr = photo.value;

                // 如果value字段为空或null
                if (valueStr == null) {
                    unreadList.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                    continue;
                }

                // 读取value字段
                List<Object> value =
                        jsonMapper.readValue(
                                valueStr,
                                new TypeReference<List<Object>>() {
                                }
                        );

                if (value.isEmpty()) {
                    unreadList.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                } else {
                    // 添加
                    int score = (int) value.get(1);
                    String note = (String) value.get(2);
                    readList.add(addPhotoToList(photo.id, photo.name, "read", score, note));
                }
            }
        }

        HashMap<String, Object> data = new HashMap<>();
        // 任务总量
        data.put("all", photosList.size());
        // 剩余任务
        data.put("remaining", unreadList.size());
        // 已完成任务
        data.put("read", readList.size());
        // 任务类型
        data.put("type", projOpt.get().type);
        data.put("max", projOpt.get().max);

        // 图片列表
        HashMap<String, List<Object>> result = new HashMap<>();
        result.put("unread", new ArrayList<>(unreadList));
        result.put("read", new ArrayList<>(readList));
        data.put("list", result);

        return new Respond<>(true, "true", data);
    }

    // 获取复审列表
    @GetMapping("/fetch_recheck_list")
    public Respond<HashMap<String, Object>> fetchRecheckList(
            @RequestParam("uid") int uid,
            @RequestParam("token") String token,
            @RequestParam("proj") String projId
    ) {
        // 验证用户
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "4", null);
        }

        // 获取项目
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 检查工程状态
        if (projOpt.get().status != 3) {
            return new Respond<>(false, "30", null);
        }

        // 获取复审人名单
        List<String> recheck = jsonMapper.readValue(projOpt.get().recheck, new TypeReference<List<String>>() {});
        if (recheck == null || recheck.isEmpty()) {
            return new Respond<>(false, "31", null);
        }
        if (!recheck.contains(String.valueOf(uid))) {
            return new Respond<>(false, "31", null);
        }

        // 获取复审照片列表
        List<EntityReviewRecheck> photos = reviewMapper.getRecheckPhotos(projId);
        // unread数组集合
        List<HashMap<String, Object>> unreadList = new ArrayList<>();
        // read数组集合
        List<HashMap<String, Object>> readList = new ArrayList<>();
        // 结果数组集合
        List<Object> result = new ArrayList<>();

        // 遍历图片列表
        for (EntityReviewRecheck recheckPhoto : photos) {
            String valueStr = recheckPhoto.value;
            // 原图片信息
            Optional<EntityReviewPhotos> photoOpt = reviewMapper.getPhotoById(recheckPhoto.photoid);
            if (photoOpt.isEmpty()) {
                continue;
            }
            EntityReviewPhotos photo = photoOpt.get();

            // 如果value字段为空或null
            if (valueStr == null) {
                unreadList.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                result.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                continue;
            }

            // 读取value字段
            HashMap<String, List<Object>> value =
                    jsonMapper.readValue(
                            valueStr,
                            new TypeReference<HashMap<String, List<Object>>>() {}
                    );

            if (!value.containsKey(String.valueOf(uid)) || value.get(String.valueOf(uid)) == null) {
                // 如果数据中没有该uid的数据
                unreadList.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
                result.add(addPhotoToList(photo.id, photo.name, "unread", 0, null));
            } else {
                // 添加
                List<Object> reviewData = value.get(String.valueOf(uid));
                int score = (int) reviewData.get(0);
                String note = (String) reviewData.get(1);
                readList.add(addPhotoToList(photo.id, photo.name, "read", score, note));
                result.add(addPhotoToList(photo.id, photo.name, "read", score, note));
            }
        }

        HashMap<String, Object> data = new HashMap<>();
        // 任务总量
        data.put("all", photos.size());
        // 剩余任务
        data.put("remaining", unreadList.size());
        // 已完成任务
        data.put("read", readList.size());
        // 任务类型
        data.put("type", projOpt.get().type);
        data.put("max", projOpt.get().max);

        data.put("list", result);

        return new Respond<>(true, "true", data);
    }

    // 获取照片列表（管理员）
    @GetMapping("fetch_photo_list_all")
    public Respond<List<EntityReviewPhotos>> fetchPhotoListAll(
            @RequestParam("adminUid") int uid,
            @RequestParam("adminToken") String token,
            @RequestParam("projId") String projId,
            @RequestParam(value = "author", required = false) String author
    ){
        // 验证用户
        if (!userMapper.checkAdmin(uid, token)) {
            return new Respond<>(false, "5", null);
        }
        if (author == null || author.isBlank()) {
            author = null;
        }
        List<EntityReviewPhotos> data = reviewMapper.getAllPhotos(projId, author);
        return new Respond<>(true, "true", data);
    }

    // 提交初始评分
    @PostMapping("/submit")
    public Respond<Integer> submitPhotos(
            @RequestParam("submit_type") String submitType,
            @RequestParam("proj") String projId,
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("photoid")||
            !body.containsKey("uid") ||
            !body.containsKey("token") ||
            !body.containsKey("score") ||
            !body.containsKey("note")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) ||
            !(body.get("token") instanceof String) ||
            !(body.get("score") instanceof Integer) ||
            !(body.get("note") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        int uid = (int) body.get("uid");
        String token = (String) body.get("token");
        // 验证用户
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "4", null);
        }

        // 获取项目
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 检查工程状态
        if (projOpt.get().status != 1) {
            return new Respond<>(false, "24", null);
        }

        int score = (int) body.get("score");
        String note = (String) body.get("note");

        // 检查评分范围
        if (score < 1 || score > projOpt.get().max) {
            return new Respond<>(false, "1", null);
        }
        // 检查批注长度
        if (note.length() > 500) {
            return new Respond<>(false, "6", null);
        }

        // 提交类型
        switch (submitType) {
            case "single":
                // 判断photoid
                if (!(body.get("photoid") instanceof Integer)) {
                    return new Respond<>(false, "1", null);
                }
                // 获取photoid
                int photoid = (int) body.get("photoid");
                // 获取photo信息
                Optional<EntityReviewPhotos> photoOpt = reviewMapper.getPhotoById(photoid);

                if (photoOpt.isEmpty()) {
                    return new Respond<>(false, "25", null);
                }
                // 比对项目
                if (!Objects.equals(photoOpt.get().proj, projId)) {
                    return new Respond<>(false, "26", null);
                }

                Boolean result = submitSingle(uid, projOpt, photoOpt, score, note);
                if (result) {
                    return new Respond<>(true, "true", 1);
                } else {
                    return new Respond<>(false, "0", 0);
                }

            case "batch":
                // 判断photoid
                if (!(body.get("photoid") instanceof List)) {
                    return new Respond<>(false, "1", 0);
                }
                List<Object> photoidList = (List<Object>) body.get("photoid");
                // 成功个数
                int successCount = 0;

                for (Object obj : photoidList) {
                    if (!(obj instanceof Integer)) {
                        continue;
                    }

                    int id = (int) obj;
                    // 获取photo信息
                    Optional<EntityReviewPhotos> photoOptBatch = reviewMapper.getPhotoById(id);
                    if (photoOptBatch.isEmpty()) {
                        continue;
                    }
                    // 比对项目
                    if (!Objects.equals(photoOptBatch.get().proj, projId)) {
                        continue;
                    }
                    Boolean batchResult = submitSingle(uid, projOpt, photoOptBatch, score, note);
                    if (!batchResult) {
                        continue;
                    }
                    successCount++;
                }
                return new Respond<>(true, "true", successCount);

            default:
                return new Respond<>(false, "1", 0);
        }
    }

    // 提交复审评分
    @PostMapping("/recheck")
    public Respond<Integer> recheck(
            @RequestParam("proj") String projId,
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("photoid")||
            !body.containsKey("uid") ||
            !body.containsKey("token") ||
            !body.containsKey("score") ||
            !body.containsKey("note")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("photoid") instanceof Integer) ||
            !(body.get("uid") instanceof Integer) ||
            !(body.get("token") instanceof String) ||
            !(body.get("score") instanceof Integer) ||
            !(body.get("note") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        int uid = (int) body.get("uid");
        String token = (String) body.get("token");
        // 验证用户
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "4", null);
        }

        // 获取项目
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 检查工程状态
        if (projOpt.get().status != 3) {
            return new Respond<>(false, "30", null);
        }

        int score = (int) body.get("score");
        String note = (String) body.get("note");

        // 检查评分范围
        if (score < 1 || score > projOpt.get().max) {
            return new Respond<>(false, "1", null);
        }
        // 检查批注长度
        if (note.isBlank()) {
            return new Respond<>(false, "1", null);
        }
        if (note.length() > 500) {
            return new Respond<>(false, "6", null);
        }

        int photoid = (int) body.get("photoid");
        // 获取photo信息
        Optional<EntityReviewRecheck> photoOpt = reviewMapper.getRecheckPhotoById(photoid);
        if (photoOpt.isEmpty()) {
            return new Respond<>(false, "25", null);
        }
        // 比对项目
        if (!Objects.equals(photoOpt.get().proj, projId)) {
            return new Respond<>(false, "26", null);
        }

        Boolean result = submitRecheckSingle(uid, photoOpt, score, note);
        if (result) {
            return new Respond<>(true, "true", 1);
        } else {
            return new Respond<>(false, "0", 0);
        }
    }

    // 提交最终评分
    @PostMapping("/final")
    public Respond<Integer> submitFinal(
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("photoid")||
            !body.containsKey("uid") ||
            !body.containsKey("token") ||
            !body.containsKey("score")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("photoid") instanceof Integer) ||
            !(body.get("uid") instanceof Integer) ||
            !(body.get("token") instanceof String) ||
            !(body.get("score") instanceof Number)
        ) {
            return new Respond<>(false, "1", null);
        }

        int uid = (int) body.get("uid");
        String token = (String) body.get("token");
        int photoid = (int) body.get("photoid");
        double score = ((Number) body.get("score")).doubleValue();

        if (!userMapper.checkAdmin(uid, token)) {
            return new Respond<>(false, "5", null);
        }
        Optional<EntityReviewRecheck> photoOpt = reviewMapper.getRecheckPhotoById(photoid);
        if (photoOpt.isEmpty()) {
            return new Respond<>(false, "25", null);
        }
        String projId = photoOpt.get().proj;
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        double scoreInTenths = score * 10;
        if (score < 1 || score > projOpt.get().max || Math.abs(scoreInTenths - Math.rint(scoreInTenths)) > 1e-9) {
            return new Respond<>(false, "33", null);
        }

        Boolean result = reviewMapper.updateFinalScore(photoid, score);
        if (result) {
            return new Respond<>(true, "true", 1);
        } else {
            return new Respond<>(false, "0", 0);
        }
    }

    /**
     * 删除某评分
     * param photoid 照片ID
     * param uid 用户ID
     */
    @PostMapping("/delete_score")
    public Respond<String> deleteScore(
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("photoid")||
            !body.containsKey("uid") ||
            !body.containsKey("adminUid") ||
            !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("photoid") instanceof Integer) ||
            !(body.get("uid") instanceof Integer) ||
            !(body.get("adminUid") instanceof Integer) ||
            !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        // 获取参数
        int photoid = (int) body.get("photoid");
        String targetUid = String.valueOf((int) body.get("uid"));
        int adminUid = (int) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 验证管理员
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 获取photo信息
        Optional<EntityReviewPhotos> photoOpt = reviewMapper.getPhotoById(photoid);
        if (photoOpt.isEmpty()) {
            return new Respond<>(false, "25", null);
        }
        // 获取工程
        String projId = photoOpt.get().proj;
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 工程类型
        int projType = projOpt.get().type;
        // 评分数据
        String originalValue = photoOpt.get().value;

        HashMap<String, Object> result = this.deleteScoreSingle(targetUid, photoid, originalValue, projType);
        return new Respond<>(
                (Boolean) result.get("status"),
                (String) result.get("message"),
                null
        );
    }

    /**
     * 批量删除工程内某评分
     * param proj 工程ID
     * param uid 用户ID
     */
    @PostMapping("/delete_score_all")
    public Respond<Integer> deleteScoreAll(
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("proj") ||
            !body.containsKey("uid") ||
            !body.containsKey("adminUid") ||
            !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }

        if (!(body.get("proj") instanceof String) ||
            !(body.get("uid") instanceof Integer) ||
            !(body.get("adminUid") instanceof Integer) ||
            !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        String projId = (String) body.get("proj");
        String targetUid = String.valueOf((int) body.get("uid"));
        int adminUid = (int) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 验证管理员
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 验证工程
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        int projType = projOpt.get().type;
        if (projType != 0 && projType != 1) {
            return new Respond<>(false, "0", null);
        }

        List<EntityReviewPhotos> photos =
                reviewMapper.getAllPhotos(projId, null);
        int deletedCount = 0;

        for (EntityReviewPhotos photo : photos) {
            // 评分原数据
            String originalValue = photo.value;
            // photoid
            int photoid = photo.id;

            // 执行
            HashMap<String, Object> result = this.deleteScoreSingle(targetUid, photoid, originalValue, projType);
            if (result.get("status").equals(false)) {
                continue;
            }
            deletedCount++;
        }

        List<EntityReviewRecheck> recheckPhotos = reviewMapper.getRecheckPhotos(projId);
        for (EntityReviewRecheck recheckPhoto : recheckPhotos) {
            HashMap<String, Object> result = this.deleteRecheckScoreSingle(
                    targetUid,
                    recheckPhoto.photoid,
                    recheckPhoto.value
            );
            if (result.get("status").equals(false)) {
                continue;
            }
            deletedCount++;
        }

        return new Respond<>(true, "true", deletedCount);
    }

    private HashMap<String, Object> deleteRecheckScoreSingle(
            String targetUid,
            int photoid,
            String originalValue
    ) {
        HashMap<String, Object> result = new HashMap<>();
        if (originalValue == null || originalValue.isEmpty()) {
            result.put("status", false);
            result.put("message", "0");
            return result;
        }

        HashMap<String, List<Object>> value =
                jsonMapper.readValue(
                        originalValue,
                        new TypeReference<HashMap<String, List<Object>>>() {}
                );
        if (!value.containsKey(targetUid)) {
            result.put("status", false);
            result.put("message", "20");
            return result;
        }

        HashMap<String, List<Object>> newValue = new HashMap<>(value);
        newValue.remove(targetUid);
        String newValueStr = jsonMapper.writeValueAsString(newValue);
        if (!reviewMapper.updateRecheckValue(photoid, newValueStr)) {
            result.put("status", false);
            result.put("message", "0");
            return result;
        }

        result.put("status", true);
        result.put("message", "true");
        return result;
    }

    @PostMapping("/delete_recheck_score")
    public Respond<String> deleteRecheckScore(
            @RequestBody HashMap<String, Object> body
    ) {
        if (!body.containsKey("photoid") ||
            !body.containsKey("uid") ||
            !body.containsKey("adminUid") ||
            !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("photoid") instanceof Integer) ||
            !(body.get("uid") instanceof Integer) ||
            !(body.get("adminUid") instanceof Integer) ||
            !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }

        int photoid = (int) body.get("photoid");
        String targetUid = String.valueOf((int) body.get("uid"));
        int adminUid = (int) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        Optional<EntityReviewRecheck> recheckOpt = reviewMapper.getRecheckPhotoById(photoid);
        if (recheckOpt.isEmpty()) {
            return new Respond<>(false, "25", null);
        }

        HashMap<String, Object> result = this.deleteRecheckScoreSingle(
                targetUid,
                photoid,
                recheckOpt.get().value
        );
        return new Respond<>(
                (Boolean) result.get("status"),
                (String) result.get("message"),
                null
        );
    }

}
