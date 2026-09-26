package com.hodastar.photosreview.service;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewProj;
import com.hodastar.photosreview.entities.EntityReviewRecheck;
import com.hodastar.photosreview.entities.EntityReviewUsers;
import com.hodastar.photosreview.mappers.ProjMapper;
import com.hodastar.photosreview.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class DataService {
    private static final Logger log = LoggerFactory.getLogger(DataService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ProjMapper projMapper;

    public DataService(JdbcTemplate jdbcTemplate, ProjMapper projMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.projMapper = projMapper;
    }
    private ObjectMapper jsonMapper = new ObjectMapper();

    // 遍历所有工程数据表
    private List<EntityReviewProj> getProjList() {
        return jdbcTemplate.query(
                "SELECT * FROM review_proj ORDER BY id ASC",
                (rs, rowNum) -> new EntityReviewProj(
                        rs.getInt("id"),
                        rs.getString("projid"),
                        rs.getString("name"),
                        rs.getInt("type"),
                        rs.getInt("max"),
                        rs.getString("task"),
                        rs.getString("recheck"),
                        rs.getString("thumbnail"),
                        rs.getInt("status"),
                        rs.getString("time"),
                        rs.getInt("display")
                )
        );
    }

    // 遍历所有初始数据表
    private List<EntityReviewPhotos> getDataList() {
        return jdbcTemplate.query(
                "SELECT * FROM review_data ORDER BY id ASC",
                (rs, rowNum) -> new EntityReviewPhotos(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("proj"),
                        rs.getString("author"),
                        rs.getString("value")
                )
        );
    }

    private List<EntityReviewPhotos> getDataListByProj(String proj) {
        return jdbcTemplate.query(
                "SELECT * FROM review_data WHERE proj = ? ORDER BY id ASC",
                (rs, rowNum) -> new EntityReviewPhotos(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("proj"),
                        rs.getString("author"),
                        rs.getString("value")
                ), proj
        );
    }

    // 遍历所有复审数据表
    private List<EntityReviewRecheck> getRecheckList() {
        return jdbcTemplate.query(
                "SELECT * FROM review_recheck ORDER BY photoid ASC",
                (rs, rowNum) -> new EntityReviewRecheck(
                        rs.getInt("photoid"),
                        rs.getString("proj"),
                        rs.getString("value"),
                        rs.getDouble("final_score")
                )
        );
    }

    private List<EntityReviewRecheck> getRecheckListByProj(String proj) {
        return jdbcTemplate.query(
                "SELECT * FROM review_recheck WHERE proj = ? ORDER BY photoid ASC",
                (rs, rowNum) -> new EntityReviewRecheck(
                        rs.getInt("photoid"),
                        rs.getString("proj"),
                        rs.getString("value"),
                        rs.getDouble("final_score")
                ), proj
        );
    }

    // 遍历所有用户表
    private List<EntityReviewUsers> getUserList() {
        return jdbcTemplate.query(
                "SELECT * FROM review_users ORDER BY uid ASC",
                (rs, rowNum) -> new EntityReviewUsers(
                        rs.getInt("uid"),
                        rs.getString("password"),
                        rs.getLong("login_time"),
                        rs.getString("allname"),
                        rs.getInt("status")
                )
        );
    }

    public HashMap<String, Object> saveAllProjData() {
        // 获取数据
        List<EntityReviewProj> projList = getProjList();
        List<EntityReviewPhotos> dataList = getDataList();
        List<EntityReviewRecheck> recheckList = getRecheckList();

        HashMap<String, Object> map = new HashMap<>();
        map.put("proj", projList);
        map.put("data", dataList);
        map.put("recheck", recheckList);
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    public void importProjData(
            EntityReviewProj proj,
            List<EntityReviewPhotos> photos,
            List<EntityReviewRecheck> rechecks,
            Path stagedProjectDirectory,
            Path targetProjectDirectory
    ) throws IOException {
        jdbcTemplate.update("DELETE FROM review_recheck WHERE proj = ?", proj.projId);
        jdbcTemplate.update("DELETE FROM review_data WHERE proj = ?", proj.projId);

        Map<Integer, EntityReviewRecheck> recheckByOldPhotoId = new LinkedHashMap<>();
        for (EntityReviewRecheck recheck : rechecks) {
            recheckByOldPhotoId.put(recheck.photoid, recheck);
        }

        for (EntityReviewPhotos photo : photos) {
            int oldPhotoId = photo.id;
            jdbcTemplate.update(
                    "INSERT INTO review_data(id, name, proj, author, value) VALUES (NULL, ?, ?, ?, ?)",
                    photo.name, photo.proj, photo.author, photo.value
            );
            Long generatedId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
            if (generatedId == null || generatedId > Integer.MAX_VALUE) {
                throw new IllegalStateException("Failed to obtain the imported photo id");
            }

            photo.id = generatedId.intValue();
            EntityReviewRecheck recheck = recheckByOldPhotoId.get(oldPhotoId);
            if (recheck != null) {
                recheck.photoid = photo.id;
                jdbcTemplate.update(
                        "INSERT INTO review_recheck(photoid, proj, value, final_score) VALUES (?, ?, ?, ?)",
                        recheck.photoid, recheck.proj, recheck.value, recheck.finalScore
                );
            }
        }
        upsertImportedProject(proj);
        replaceProjectDirectory(stagedProjectDirectory, targetProjectDirectory);
    }

    private void upsertImportedProject(EntityReviewProj proj) {
        jdbcTemplate.update(
                """
                INSERT INTO review_proj(
                    projid, name, type, max, task, recheck, thumbnail, status, time, display
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(projid) DO UPDATE SET
                    name = excluded.name,
                    type = excluded.type,
                    max = excluded.max,
                    task = excluded.task,
                    recheck = excluded.recheck,
                    thumbnail = excluded.thumbnail,
                    status = excluded.status,
                    time = excluded.time,
                    display = excluded.display
                """,
                proj.projId, proj.name, proj.type, proj.max, proj.task,
                proj.recheck, proj.thumbnail, proj.status, proj.time, proj.display
        );
    }

    private void replaceProjectDirectory(Path stagedDirectory, Path targetDirectory) throws IOException {
        Path target = targetDirectory.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || stagedDirectory == null || !Files.isDirectory(stagedDirectory)) {
            throw new IOException("Invalid project directory");
        }

        Files.createDirectories(parent);
        Path backup = parent.resolve(".import-backup-" + UUID.randomUUID()).normalize();
        boolean hadExistingDirectory = Files.exists(target);
        if (hadExistingDirectory) {
            Files.move(target, backup);
        }

        try {
            Files.move(stagedDirectory, target);
        } catch (IOException ex) {
            if (hadExistingDirectory && Files.exists(backup)) {
                Files.move(backup, target);
            }
            throw ex;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            FileUtil.removeDir(backup.toString());
            return;
        }
        registerDirectoryCleanup(target, backup, hadExistingDirectory);
    }

    private void registerDirectoryCleanup(Path target, Path backup, boolean hadExistingDirectory) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                try {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        FileUtil.removeDir(backup.toString());
                        return;
                    }
                    FileUtil.removeDir(target.toString());
                    if (hadExistingDirectory && Files.exists(backup)) {
                        Files.move(backup, target);
                    }
                } catch (Exception ex) {
                    log.error("Failed to finish project directory replacement for {}", target, ex);
                }
            }
        });
    }

    public HashMap<String, Object> saveProjData(String projId) {
        // 获取数据
        Optional<EntityReviewProj> projOpt = projMapper.getProjById(projId);
        if (projOpt.isEmpty()) return null;
        List<EntityReviewPhotos> dataList = getDataListByProj(projId);
        List<EntityReviewRecheck> recheckList = getRecheckListByProj(projId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("proj", projOpt.get());
        map.put("data", dataList);
        map.put("recheck", recheckList);
        return map;
    }
}
