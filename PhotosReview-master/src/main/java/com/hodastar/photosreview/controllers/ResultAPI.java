package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewProj;
import com.hodastar.photosreview.entities.EntityReviewRecheck;
import com.hodastar.photosreview.mappers.ProjMapper;
import com.hodastar.photosreview.mappers.ResultMapper;
import com.hodastar.photosreview.mappers.ReviewMapper;
import com.hodastar.photosreview.mappers.UserMapper;
import com.hodastar.photosreview.utils.FileUtil;
import com.hodastar.photosreview.utils.OutputPhotosPath;
import com.hodastar.photosreview.utils.Respond;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.hodastar.photosreview.utils.Utilities.*;

/**
 * 管理端结果统计接口。
 *
 * <p>当前负责汇总工程初审结果，并在接口内部完成争议照片判定。统计口径以单张照片的
 * 初审平均分为基础；争议判定只对有效评分人数不少于四人的照片执行。</p>
 */
@RestController
@RequestMapping("/api/result")
public class ResultAPI {
    // 同一管理员对同一工程重新获取初审结果的冷却时间，单位为毫秒。
    private static final long COOLDOWN_MS = 60_000L;
    // 参与争议判定所需的最少有效评分人数。
    private static final int MIN_DISPUTE_REVIEWERS = 4;
    // 分歧指数超过该阈值时，将照片判定为争议照片。
    private static final double DISPUTE_THRESHOLD = 0.4;
    // 争议原因使用稳定代码返回，由前端按当前语言转换为展示文本。
    private static final String DISPUTE_REASON_LARGE_DISPERSION = "large_dispersion";
    private static final String DISPUTE_REASON_LARGE_RANGE = "large_range";
    private static final String DISPUTE_REASON_POLARIZATION = "polarization";
    private static final String DISPUTE_REASON_OUTLIER = "outlier";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProjMapper projMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ResultMapper resultMapper;
    // 用于解析数据库中以 JSON 字符串保存的评分数据。
    private final ObjectMapper jsonMapper = new ObjectMapper();
    // 冷却键格式为“管理员 UID:工程 ID”，值为最近一次成功开始统计的时间戳。
    private final Map<String, Long> preliminaryResultCooldown = new ConcurrentHashMap<>();
    private final Map<String, Long> finalResultCooldown = new ConcurrentHashMap<>();

    /**
     * 从照片评分字段中提取有效分数。
     *
     * <p>普通评审模式的数据结构为“评分人员 UID -> [分数, 评语]”；
     * 筛片模式的数据结构为“[评分人员 UID, 分数, 评语]”。</p>
     *
     * @param value 数据库中的评分 JSON 字符串
     * @param projectType 工程类型，0 为普通评审，1 为筛片模式
     * @param maxScore 工程满分 Max
     * @return 范围在 [0, Max] 内的有效分数
     */
    private List<Double> extractValidScores(String value, int projectType, int maxScore) {
        List<Double> scores = new ArrayList<>();
        if (value == null || value.isBlank() || maxScore <= 0) {
            return scores;
        }

        try {
            if (projectType == 0) {
                // 普通评审可包含多名评分人员，因此遍历映射中的全部评分记录。
                Map<String, List<Object>> reviewValues = jsonMapper.readValue(
                        value,
                        new TypeReference<HashMap<String, List<Object>>>() {}
                );
                for (List<Object> review : reviewValues.values()) {
                    if (review != null && !review.isEmpty()) {
                        addValidScore(scores, review.get(0), maxScore);
                    }
                }
            } else if (projectType == 1) {
                // 筛片模式为单评结构，分数位于数组下标 1。
                List<Object> screeningValue = jsonMapper.readValue(
                        value,
                        new TypeReference<List<Object>>() {}
                );
                if (screeningValue.size() > 1) {
                    addValidScore(scores, screeningValue.get(1), maxScore);
                }
            }
        } catch (Exception ignored) {
            // 单条历史数据格式异常时忽略该条，避免中断整个工程的统计。
        }
        return scores;
    }

    /**
     * 校验并加入单个评分。
     *
     * @param scores 有效评分集合
     * @param scoreValue 待校验的评分值
     * @param maxScore 工程满分 Max
     */
    private void addValidScore(List<Double> scores, Object scoreValue, int maxScore) {
        if (!(scoreValue instanceof Number number)) {
            return;
        }
        double score = number.doubleValue();
        if (Double.isFinite(score) && score >= 0.0 && score <= maxScore) {
            scores.add(score);
        }
    }

    /**
     * 计算分歧指数，返回值限制在 [0, 1]。
     *
     * <p>计算公式：标准差标准化值 × 45% + 极差标准化值 × 35%
     * + 两极化程度 × 20%。标准差以 Max/2 标准化，极差以 Max 标准化。</p>
     */
    private double calculateDisagreementIndex(List<Double> scores, int maxScore) {
        double average = mean(scores);
        double standardDeviation = Math.sqrt(populationVariance(scores, average));
        double min = Collections.min(scores);
        double max = Collections.max(scores);
        // 对 [0, Max] 范围内的分数，理论最大标准差为 Max/2。
        double standardDeviationNormalized = clamp(standardDeviation / (maxScore / 2.0));
        // 极差除以 Max 后自然落在 [0, 1]。
        double rangeNormalized = clamp((max - min) / maxScore);
        double polarization = calculatePolarization(scores, maxScore);
        return clamp(
                standardDeviationNormalized * 0.45
                        + rangeNormalized * 0.35
                        + polarization * 0.20
        );
    }

    /**
     * 根据分歧指数的组成项和离群检测结果生成争议原因代码。
     *
     * <p>各组成项沿用总体争议阈值 0.4。由于总体指数是三个组成项的加权平均值，
     * 当总体指数超过阈值时，至少会有一个组成项超过同一阈值。</p>
     */
    private List<String> calculateDisputeReasons(List<Double> scores, int maxScore, boolean hasOutlier) {
        List<String> reasons = new ArrayList<>();
        double average = mean(scores);
        double standardDeviation = Math.sqrt(populationVariance(scores, average));
        double standardDeviationNormalized = clamp(standardDeviation / (maxScore / 2.0));
        double rangeNormalized = clamp((Collections.max(scores) - Collections.min(scores)) / maxScore);
        double polarization = calculatePolarization(scores, maxScore);

        if (standardDeviationNormalized > DISPUTE_THRESHOLD) {
            reasons.add(DISPUTE_REASON_LARGE_DISPERSION);
        }
        if (rangeNormalized > DISPUTE_THRESHOLD) {
            reasons.add(DISPUTE_REASON_LARGE_RANGE);
        }
        if (polarization > DISPUTE_THRESHOLD) {
            reasons.add(DISPUTE_REASON_POLARIZATION);
        }
        if (hasOutlier) {
            reasons.add(DISPUTE_REASON_OUTLIER);
        }
        return reasons;
    }

    /**
     * 计算评分的两极化程度。
     *
     * <p>低分组定义为不高于 Max × 40%，高分组定义为不低于 Max × 60%。
     * 最终值由两端评分覆盖率、两组人数均衡度和两组均值间距相乘得到；
     * 任一端没有评分时视为未形成两极化。</p>
     *
     * @return 范围在 [0, 1] 内的两极化程度
     */
    private double calculatePolarization(List<Double> scores, int maxScore) {
        List<Double> lowScores = scores.stream()
                .filter(score -> score <= maxScore * 0.4)
                .toList();
        List<Double> highScores = scores.stream()
                .filter(score -> score >= maxScore * 0.6)
                .toList();
        int extremeCount = lowScores.size() + highScores.size();
        if (lowScores.isEmpty() || highScores.isEmpty() || extremeCount == 0) {
            return 0.0;
        }

        // 覆盖率表示落在低分端或高分端的评分占比。
        double coverage = extremeCount / (double) scores.size();
        // 均衡度在两端人数相等时为 1，人数越失衡则越接近 0。
        double balance = 2.0 * Math.min(lowScores.size(), highScores.size()) / extremeCount;
        // 间距以 Max 标准化，表示高低两组平均分相距多远。
        double separation = (mean(highScores) - mean(lowScores)) / maxScore;
        return clamp(coverage * balance * separation);
    }

    /**
     * 判断评分集合中是否存在离群值。
     *
     * <p>先使用四分位距法检查 1.5 × IQR 围栏；若未检出，再使用基于中位数
     * 绝对偏差的修正 Z 分数检查。MAD 为零时，以偏离中位数超过 Max × 35%
     * 作为兜底规则。</p>
     *
     * @return 存在任一离群评分时返回 true
     */
    private boolean hasOutlier(List<Double> scores, int maxScore) {
        // 四分位数和中位数的计算都要求数据按升序排列。
        List<Double> sorted = new ArrayList<>(scores);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        List<Double> lowerHalf = sorted.subList(0, middle);
        List<Double> upperHalf = sorted.subList((sorted.size() + 1) / 2, sorted.size());
        double q1 = median(lowerHalf);
        double q3 = median(upperHalf);
        double iqr = q3 - q1;
        // Tukey 围栏：低于 Q1 - 1.5×IQR 或高于 Q3 + 1.5×IQR 即为离群。
        double lowerFence = q1 - 1.5 * iqr;
        double upperFence = q3 + 1.5 * iqr;
        if (sorted.stream().anyMatch(score -> score < lowerFence || score > upperFence)) {
            return true;
        }

        // MAD 对少量极端值不敏感，适合作为第二层稳健离群检测。
        double median = median(sorted);
        List<Double> deviations = sorted.stream()
                .map(score -> Math.abs(score - median))
                .sorted()
                .toList();
        double mad = median(deviations);
        if (mad > 0.0) {
            // 0.6745 为正态分布下的尺度修正常数，3.5 为常用离群阈值。
            return sorted.stream()
                    .anyMatch(score -> 0.6745 * Math.abs(score - median) / mad > 3.5);
        }
        // 所有绝对偏差的中位数为零时，改用相对于 Max 的固定距离兜底。
        return sorted.stream().anyMatch(score -> Math.abs(score - median) > maxScore * 0.35);
    }

    /**
     * 计算直方图数据的索引位置，返回值限制在 [0, (Max-1)*5]。
     */
    private int calculateHistogramIndex(double score, int maxScore) {
        int index = (int) ((score - 1.0) * 5);
        return Math.max(0, Math.min(index, (maxScore - 1) * 5));
    }

    private HashMap<String, Object> loadPhotoData(EntityReviewPhotos photo, EntityReviewProj proj) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("proj", proj.name);
        result.put("project_type", proj.type);
        result.put("max_score", proj.max);
        result.put("photoid", photo.id);
        result.put("name", photo.name);
        result.put("author", photo.author);
        result.put("is_recheck", false);

        JsonNode root = jsonMapper.readTree(photo.value);
        // 判断value的json类型
        if (root.isObject()) {
            // 审片模式
            HashMap<String, List<Object>> value =
                    jsonMapper.readValue(
                            photo.value,
                            new TypeReference<HashMap<String, List<Object>>>() {}
                    );
            result.put("preliminary", value);
        } else if (root.isArray()) {
            // 筛片模式
            List<Object> value =
                    jsonMapper.readValue(
                            photo.value,
                            new TypeReference<List<Object>>() {}
                    );
            result.put("preliminary", value);
        } else {
            return null;
        }

        // 检测是否有复审
        Optional<EntityReviewRecheck> recheckOpt = reviewMapper.getRecheckPhotoById(photo.id);
        if (recheckOpt.isPresent()) {
            HashMap<String, List<Object>> value =
                    jsonMapper.readValue(
                            recheckOpt.get().value,
                            new TypeReference<HashMap<String, List<Object>>>() {}
                    );
            result.put("is_recheck", true);
            result.put("recheck", value);
            result.put("final_score", recheckOpt.get().finalScore);
        } else {
            result.put("is_recheck", false);
        }

        return result;
    }

    /**
     * 获取单张照片的数据
     *
     * @param uid
     * @param token
     * @param photoid 图片id
     * @return
     */
    @GetMapping("/fetch_photo_data")
    public Respond<HashMap<String, Object>> fetchPhotoData(
            @RequestParam("adminUid") int uid,
            @RequestParam("adminToken") String token,
            @RequestParam("photoid") int photoid
    ) {
        // 验证用户
        if (!userMapper.checkAdmin(uid, token)) {
            return new Respond<>(false, "5", null);
        }

        Optional<EntityReviewPhotos> photoOpt = reviewMapper.getPhotoById(photoid);
        if (photoOpt.isEmpty()) {
            return new Respond<>(false, "25", null);
        }

        Optional<EntityReviewProj> projOpt = projMapper.getProjById(photoOpt.get().proj);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        HashMap<String, Object> data = loadPhotoData(photoOpt.get(), projOpt.get());
        if (data == null) {
            return  new Respond<>(false, "0", null);
        }

        return new Respond<>(true, "true", data);
    }

    /**
     * 获取指定工程的初审总体数据和争议照片数组。
     *
     * @param projId 工程 ID
     * @param adminUid 管理员 UID
     * @param adminToken 管理员身份令牌
     * @return 初审统计结果；鉴权、工程不存在或冷却未结束时返回对应错误
     */
    @GetMapping("/preliminary")
    public Respond<HashMap<String, Object>> getPreliminaryResult(
            @RequestParam("projId") String projId,
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        // 结果数据仅允许通过管理员身份读取。
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 在统计前确认工程存在，避免继续查询无效工程的照片数据。
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        // 冷却按管理员和工程分别计时，不影响其他管理员或其他工程。
        String cooldownKey = adminUid + ":" + projId;
        long now = System.currentTimeMillis();
        Long lastFetchTime = preliminaryResultCooldown.get(cooldownKey);
        if (lastFetchTime != null && now - lastFetchTime < COOLDOWN_MS) {
            return new Respond<>(false, "28", null);
        }
        preliminaryResultCooldown.put(cooldownKey, now);

        EntityReviewProj proj = projOpt.get();
        // preliminaryScores 保存每张已评分照片的初审平均分，用于计算总体统计值。
        List<EntityReviewPhotos> photos = reviewMapper.getAllPhotos(projId, null);
        List<Double> preliminaryScores = new ArrayList<>();
        List<HashMap<String, Object>> disputePhotos = new ArrayList<>();

        for (EntityReviewPhotos photo : photos) {
            // 按工程模式解析评分，同时过滤格式无效或超出 [0, Max] 的分数。
            List<Double> scores = extractValidScores(photo.value, proj.type, proj.max);
            if (scores.isEmpty()) {
                // 没有有效评分的照片不计入“已评分总量”和后续总体统计。
                continue;
            }

            // 单张照片的初审分数取该照片全部有效评分的算术平均值。
            double photoScore = mean(scores);
            preliminaryScores.add(photoScore);

            // 少于四名评分人员时不进行分歧或离群判断。
            if (scores.size() >= MIN_DISPUTE_REVIEWERS) {
                double disputeIndex = calculateDisagreementIndex(scores, proj.max);
                boolean hasOutlier = hasOutlier(scores, proj.max);
                List<String> disputeReasons = calculateDisputeReasons(scores, proj.max, hasOutlier);
                // 分歧指数超限或存在离群值，任一条件成立即列为争议照片。
                if (disputeIndex > DISPUTE_THRESHOLD || hasOutlier) {
                    HashMap<String, Object> disputePhoto = new HashMap<>();
                    disputePhoto.put("photoid", photo.id);
                    disputePhoto.put("name", photo.name);
                    disputePhoto.put("author", photo.author);
                    disputePhoto.put("value", roundOne(photoScore));
                    disputePhoto.put("disputeIndex", roundTwo(disputeIndex));
                    disputePhoto.put("disputeReasons", disputeReasons);
                    disputePhotos.add(disputePhoto);
                }
            }
        }

        // 以下四项均以单张照片的初审平均分为统计样本。
        List<Double> sortedPreliminaryScores = new ArrayList<>(preliminaryScores);
        Collections.sort(sortedPreliminaryScores);
        double preliminaryAverage = mean(preliminaryScores);
        double preliminaryMedian = median(sortedPreliminaryScores);
        int scoredTotal = preliminaryScores.size();
        int disputeCount = disputePhotos.size();
        // 接口中的争议率使用百分数表达，例如 7.1 表示 7.1%。
        double disputeRate = scoredTotal == 0 ? 0.0 : disputeCount * 100.0 / scoredTotal;

        // 数值展示精度统一为 0.1，及格线固定为 Max 的 60%。
        HashMap<String, Object> data = new HashMap<>();
        data.put("projectId", proj.projId);
        data.put("projectName", proj.name);
        data.put("max", proj.max);
        data.put("passingScore", roundOne(proj.max * 0.6));
        data.put("scoredTotal", scoredTotal);
        data.put("disputeCount", disputeCount);
        data.put("disputeRate", roundOne(disputeRate));
        data.put("preliminaryAverage", roundTwo(preliminaryAverage));
        data.put("preliminaryMedian", roundTwo(preliminaryMedian));
        data.put("disputePhotos", disputePhotos);
        return new Respond<>(true, "success", data);
    }

    // 计算并存储结果数据
    @PostMapping("/build_overall_result")
    public Respond<HashMap<String, Object>> computeAndStoreResults(
            @RequestBody Map<String, Object> body
    ) {
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


        // 结果数据仅允许通过管理员身份读取。
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        // 在统计前确认工程存在，避免继续查询无效工程的照片数据。
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }

        // 冷却按管理员和工程分别计时，不影响其他管理员或其他工程。
        String cooldownKey = adminUid + ":" + projId;
        long now = System.currentTimeMillis();
        Long lastFetchTime = finalResultCooldown.get(cooldownKey);
        if (lastFetchTime != null && now - lastFetchTime < COOLDOWN_MS) {
            return new Respond<>(false, "28", null);
        }
        finalResultCooldown.put(cooldownKey, now);

        // 本工程所有图片列表
        List<EntityReviewPhotos> photos = reviewMapper.getAllPhotos(projId, null);
        // 工程类型
        int projectType = projOpt.get().type;
        // 工程满分
        int maxScore = projOpt.get().max;
        // 计数
        int totalPhotos = 0;
        //争议计数
        int disputeCount = 0;
        // 最终分数结果存储
        List<Double> finalScores = new ArrayList<>();
        HashMap<String, List<Double>> finalScoreMap = new HashMap<>();
        // 分数直方图数据存储
        List<Integer> scoreHistogram = new ArrayList<>(Collections.nCopies((maxScore - 1) * 5 + 1, 0));
        // 争议原因直方图数据存储
        int[] disputeReasonHistogram = {0, 0, 0, 0};
        // 指标存储
        HashMap<String, Object> metrics = new HashMap<>();

        for (EntityReviewPhotos photo : photos) {
            // 获取有效分数
            List<Double> photoScores = extractValidScores(photo.value, projectType, maxScore);
            // 作者
            String author = photo.author;
            if (photoScores.isEmpty()) {
                continue;
            }

            // 判断争议
            if (photoScores.size() >= MIN_DISPUTE_REVIEWERS) {
                double disputeIndex = calculateDisagreementIndex(photoScores, maxScore);
                boolean hasOutlier = hasOutlier(photoScores, maxScore);
                List<String> disputeReasons = calculateDisputeReasons(photoScores, maxScore, hasOutlier);
                // 计数
                if (disputeIndex > DISPUTE_THRESHOLD || hasOutlier) {
                    disputeCount++;
                    // 计数
                    for (String disputeReason : disputeReasons) {
                        switch (disputeReason) {
                            case DISPUTE_REASON_LARGE_DISPERSION:
                                disputeReasonHistogram[0]++;
                                break;

                            case DISPUTE_REASON_LARGE_RANGE:
                                disputeReasonHistogram[1]++;
                                break;

                            case DISPUTE_REASON_POLARIZATION:
                                disputeReasonHistogram[2]++;
                                break;

                            case DISPUTE_REASON_OUTLIER:
                                disputeReasonHistogram[3]++;
                                break;
                        }
                    }
                }
                }

            // 最终分数
            double finalScore;
            // 获取复审
            Optional<EntityReviewRecheck> recheckOpt = reviewMapper.getRecheckPhotoById(photo.id);
            if (recheckOpt.isEmpty()) {
                // 没有复审时，最终分数为初审平均分
                finalScore = mean(photoScores);
            } else {
                // 终审分数
                double originalFinalScore = recheckOpt.get().finalScore;
                if (originalFinalScore >= 1 && originalFinalScore <= maxScore) {
                    // 有复审有终审时，最终分数为复审分数
                    finalScore = originalFinalScore;
                } else {
                    // 有复审但终审分数无效时，最终分数为初审平均分
                    finalScore = mean(photoScores);
                }
            }

            finalScore = roundOne(finalScore);
            // 添加到最终分数列表
            finalScores.add(finalScore);
            if (finalScoreMap.containsKey(author)) {
                finalScoreMap.get(author).add(finalScore);
            } else {
                List<Double> authorScores = new ArrayList<>();
                authorScores.add(finalScore);
                finalScoreMap.put(author, authorScores);
            }
            // 添加到分数直方图
            int histogramIndex = calculateHistogramIndex(finalScore, maxScore);
            scoreHistogram.set(histogramIndex, scoreHistogram.get(histogramIndex) + 1);

            totalPhotos++;
        }

        // 计算总体指标
        double overallAverage = mean(finalScores);
        double overallMedian = median(finalScores);
        // 及格率
        double passingScore = roundOne(maxScore * 0.6);
        double passingRate = totalPhotos == 0 ? 0.0 :
                finalScores.stream().filter(score -> score >= passingScore).count() * 100.0 / totalPhotos;
        // 满分率
        double fullScoreRate = totalPhotos == 0 ? 0.0 :
                finalScores.stream().filter(score -> score == maxScore).count() * 100.0 / totalPhotos;

        metrics.put("overallAverage", roundTwo(overallAverage));
        metrics.put("overallMedian", roundTwo(overallMedian));
        metrics.put("totalPhotos", totalPhotos);
        metrics.put("disputeCount", disputeCount);
        metrics.put("disputeRate", totalPhotos == 0 ? 0.0 : roundOne(disputeCount * 100.0 / totalPhotos));
        metrics.put("passingRate", roundOne(passingRate));
        metrics.put("fullScoreRate", roundOne(fullScoreRate));

        // 每个作者均分列表
        HashMap<String, Double> averageMap = new HashMap<>();
        // 每个作者总分列表
        HashMap<String, Double> sumMap = new HashMap<>();
        finalScoreMap.forEach((author, scores) -> {
            double authorAverage = mean(scores);
            double authorSum = scores.stream().mapToDouble(Double::doubleValue).sum();
            averageMap.put(author, roundTwo(authorAverage));
            sumMap.put(author, roundTwo(authorSum));
        });

        List<HashMap<String, Double>> sortedAverageAuthors = averageMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    HashMap<String, Double> map = new HashMap<>();
                    map.put(entry.getKey(), entry.getValue());
                    return map;
                })
                .toList();

        List<HashMap<String, Double>> sortedSumAuthors = sumMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    HashMap<String, Double> map = new HashMap<>();
                    map.put(entry.getKey(), entry.getValue());
                    return map;
                })
                .toList();

        // 存储结果
        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("metrics", metrics);
        resultData.put("scoreHistogram", scoreHistogram);
        resultData.put("disputeReasonHistogram", disputeReasonHistogram);
        resultData.put("topAverageAuthors", sortedAverageAuthors.stream().toList());
        resultData.put("topSumAuthors", sortedSumAuthors.stream().toList());
        String resultJson = jsonMapper.writeValueAsString(resultData);

        Boolean result = resultMapper.insertResult(projId, resultJson);
        if (result) {
            return new Respond<>(true, "success", resultData);
        } else {
            return new Respond<>(false, "0", null);
        }
    }

    // 获取结果数据
    @GetMapping("/fetch_result")
    public Respond<HashMap<String, Object>> fetchResult(
            @RequestParam("projId") String projId,
            @RequestParam("uid") int uid,
            @RequestParam("token") String token
    ) {
        // 验证用户
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "5", null);
        }
        // 在统计前确认工程存在，避免继续查询无效工程的照片数据。
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 检查工程状态
        if (projOpt.get().status != 2 && projOpt.get().status != 4) {
            return new Respond<>(false, "34", null);
        }

        String value = resultMapper.getResultByProjId(projId);
        if (value.isBlank()) {
            return new Respond<>(false, "29", null);
        }

        HashMap<String, Object> resultData = jsonMapper.readValue(
                value,
                new TypeReference<HashMap<String, Object>>() {}
        );
        resultData.put("projectName", projOpt.get().name);

        return new Respond<>(true, "success", resultData);
    }

    // 按作者获取结果数据
    @GetMapping("/fetch_result_by_author")
    public Respond<HashMap<String, Object>> fetchResultByAuthor(
            @RequestParam("projId") String projId,
            @RequestParam("author") String author,
            @RequestParam("uid") int uid,
            @RequestParam("token") String token
    ) {
        // 验证用户
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "5", null);
        }
        // 在统计前确认工程存在，避免继续查询无效工程的照片数据。
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            return new Respond<>(false, "14", null);
        }
        // 检查工程状态
        if (projOpt.get().status != 2 && projOpt.get().status != 4) {
            return new Respond<>(false, "34", null);
        }

        // 获取图片
        List<EntityReviewPhotos> photos = resultMapper.getPhotosByAuthor(projId, author);
        if (photos.isEmpty()) {
            return new Respond<>(false, "35", null);
        }

        List<HashMap<String, Object>> photoDataList = new ArrayList<>();
        for (EntityReviewPhotos photo : photos) {
            HashMap<String, Object> photoData = loadPhotoData(photo, projOpt.get());
            if (photoData != null) {
                photoDataList.add(photoData);
            }
        }
        if (photoDataList.isEmpty()) {
            return new Respond<>(false, "35", null);
        }

        HashMap<String, Object> resultData = new HashMap<>();
        resultData.put("proj", projOpt.get().name);
        resultData.put("photos", photoDataList);
        return new Respond<>(true, "success", resultData);
    }

    // 根据分数区间导出图片zip
    @GetMapping("/export_photos_by_score_range")
    public void exportPhotosByScoreRange(
            @RequestParam("projId") String projId,
            @RequestParam("minScore") double minScore,
            @RequestParam("maxScore") double maxScore,
            @RequestParam("uid") int uid,
            @RequestParam("token") String token,
            HttpServletResponse response
    ) throws IOException {
        // 验证用户
        if (!userMapper.checkAdmin(uid, token)) {
            response.setStatus(403);
            return;
        }
        // 在统计前确认工程存在，避免继续查询无效工程的照片数据。
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) {
            response.setStatus(403);
            return;
        }
        // 检查工程状态
        if (projOpt.get().status != 2 && projOpt.get().status != 4) {
            response.setStatus(403);
            return;
        }

        if (!Double.isFinite(minScore)
                || !Double.isFinite(maxScore)
                || minScore < 1
                || minScore > maxScore
                || maxScore > projOpt.get().max) {
            response.setStatus(400);
            return;
        }

        // 文件名列表
        List<String> files = new ArrayList<>();
        // 获取所有图片
        List<EntityReviewPhotos> photos = reviewMapper.getAllPhotos(projId, null);
        for (EntityReviewPhotos photo : photos) {
            // 初审评分
            List<Double> photoScores = extractValidScores(photo.value, projOpt.get().type, projOpt.get().max);
            if (photoScores.isEmpty()) {
                continue;
            }
            // 最终分数
            double finalScore;
            // 获取复审
            Optional<EntityReviewRecheck> recheckOpt = reviewMapper.getRecheckPhotoById(photo.id);
            if (recheckOpt.isEmpty()) {
                // 没有复审时，最终分数为初审平均分
                finalScore = mean(photoScores);
            } else {
                // 终审分数
                double originalFinalScore = recheckOpt.get().finalScore;
                if (originalFinalScore >= 1 && originalFinalScore <= projOpt.get().max) {
                    // 有复审有终审时，最终分数为复审分数
                    finalScore = originalFinalScore;
                } else {
                    // 有复审但终审分数无效时，最终分数为初审平均分
                    finalScore = mean(photoScores);
                }
            }

            finalScore = roundOne(finalScore);

            if (finalScore >= minScore && finalScore <= maxScore) {
                files.add(photo.name);
            }
        }

        long now = System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String time = Instant.ofEpochMilli(now)
                .atZone(ZoneId.systemDefault())
                .format(formatter);

        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=output_photos_by_scores_" + time + ".zip"
        );

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (String file : files) {
                Path filePath = OutputPhotosPath.imgPath(projId, file);

                if (!Files.exists(filePath)) {
                    continue;
                }

                FileUtil.zipFileList(filePath, zos);
            }
        }
    }
}
