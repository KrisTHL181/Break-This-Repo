package com.hodastar.photosreview.mappers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewProj;
import com.hodastar.photosreview.utils.Utilities;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Repository
public class ProjMapper {
    private final JdbcTemplate jdbcTemplate;

    public ProjMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 获取工程列表
    public List<EntityReviewProj> getProjList(int page) {
        int[] a = Utilities.calculateOffsetLimit(page, 10);
        int offset = a[0];
        int limit = a[1];
        return jdbcTemplate.query(
                "SELECT * FROM review_proj ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    return new EntityReviewProj(
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
                    );
                },
                limit, offset
        );
    }

    public List<EntityReviewProj> getProjList() {
        return getProjList(1);
    }

    /**
     * 新建工程
     * @param name 工程名称
     * @param type 工程类型
     * @param thumbnail 工程缩略图
     * @return 新建结果
     */
    public Boolean createProj(String projId, String name, int type, int max, String thumbnail) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO review_proj(projid, name, type, max, task, recheck, thumbnail, status, time, display) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    projId,
                    name,
                    type,
                    max,
                    "{}",
                    "[]",
                    thumbnail,
                    0,
                    Utilities.nowTimeString(),
                    1
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 检查工程是否重名
    public Boolean isProjNameExist(String name) {
        Boolean isExist = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM review_proj WHERE name = ?)",
                Boolean.class,
                name
        );
        return Boolean.TRUE.equals(isExist);
    }

    // 获取该工程的总量
    public int getProjDataCount(String projId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_data WHERE proj = ?",
                Integer.class,
                projId
        );
        return count != null ? count : 0;
    }

    // 删除工程
    public Boolean deleteProj(String projId) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "DELETE FROM review_proj WHERE projid = ?",
                    projId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 通过id获取单个工程
     * @param projId 工程ID
     * @return 工程信息
     */
    public Optional<EntityReviewProj> getProjById(String projId) {
        try {
            List<EntityReviewProj> list = jdbcTemplate.query(
                    "SELECT * FROM review_proj WHERE projid = ?",
                    (rs, rowNum) -> {
                        return new EntityReviewProj(
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
                        );
                    },
                    projId
            );
            return list.stream().findFirst();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * 通过name获取单个工程
     * @param name
     * @return 工程信息
     */
    public Optional<EntityReviewProj> getProjByName(String name) {
        try {
            List<EntityReviewProj> list = jdbcTemplate.query(
                    "SELECT * FROM review_proj WHERE name = ?",
                    (rs, rowNum) -> {
                        return new EntityReviewProj(
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
                        );
                    },
                    name);
            return list.stream().findFirst();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // 通过工程名获取工程id
    public String getProjIdByName(String name) {
        try {
            String projId = jdbcTemplate.queryForObject(
                    "SELECT projid FROM review_proj WHERE name = ?",
                    String.class,
                    name
            );
            return projId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 通过工程id获取工程名
    public String getProjNameById(String projId) {
        try {
            String name = jdbcTemplate.queryForObject(
                    "SELECT name FROM review_proj WHERE projid = ?",
                    String.class,
                    projId
            );
            return name;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 修改工程
     * @param projId 工程ID
     * @param name 工程名称
     * @param thumbnail 工程缩略图
     * @param status 工程状态
     * @param display 工程是否显示
     * @return 修改结果
     */
    public Boolean updateProj(String projId, String name, String thumbnail, int status, int display) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "UPDATE review_proj SET name = ?, thumbnail = ?, status = ?, display = ? WHERE projid = ?",
                    name, thumbnail, status, display, projId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 修改工程任务
     * @param projId 工程ID
     * @param task 工程任务
     * @return 修改结果
     */
    public Boolean updateProjTask(String projId, String task, String recheck) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "UPDATE review_proj SET task = ?, recheck = ?  WHERE projid = ?",
                    task, recheck, projId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 添加图片
     * @param projId 工程名称
     * @param author 作者
     * @param name 文件名称
     * @param value 值
     */
    public Boolean addPhoto(String projId, String author, String name, String value) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "INSERT INTO review_data(id, name, proj, author, value) VALUES (null, ?, ?, ?, ?)",
                    name, projId, author, value
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 通过id获取照片
     * @param id 照片id
     * @return 照片信息
     */
    public Optional<EntityReviewPhotos> getPhotoById(int id) {
        try {
            EntityReviewPhotos photo = jdbcTemplate.queryForObject(
                    "SELECT * FROM review_data WHERE id = ?",
                    (rs, rowNum) -> {
                        return new EntityReviewPhotos(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("proj"),
                                rs.getString("author"),
                                rs.getString("value")
                        );
                    },
                    id
            );
            return Optional.ofNullable(photo);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * 通过id删除照片
     * @param id 照片id
     * @return 删除结果
     */
    public Boolean deletePhotoById(int id) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "DELETE FROM review_data WHERE id = ?",
                    id
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 通过id列表删除照片
     * @param ids 照片id
     * @return 删除结果
     */
    public int deletePhotoByIds(List<Integer> ids) {
        try {
            String sql = "DELETE FROM review_data WHERE id IN (" +
                    String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new)) +
                    ")";
            return jdbcTemplate.update(sql);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<HashMap<String, Object>> getRecheckPhotoList(String projId) {
        return jdbcTemplate.query(
                """
                SELECT r.photoid, d.name, d.author, r.value, r.final_score
                FROM review_recheck r
                LEFT JOIN review_data d ON d.id = r.photoid AND d.proj = r.proj
                WHERE r.proj = ?
                ORDER BY r.photoid ASC
                """,
                (rs, rowNum) -> {
                    HashMap<String, Object> item = new HashMap<>();
                    item.put("photoid", rs.getInt("photoid"));
                    item.put("name", rs.getString("name"));
                    item.put("author", rs.getString("author"));
                    item.put("value", rs.getString("value"));
                    item.put("final_score", rs.getDouble("final_score"));
                    return item;
                },
                projId
        );
    }

    public Boolean addRecheck(int photoId, String projId) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    """
                    INSERT INTO review_recheck(photoid, proj, value, final_score)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(photoid) DO UPDATE SET
                        proj = excluded.proj
                    """,
                    photoId, projId, "{}", -1
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean deleteRecheck(int photoId, String projId) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "DELETE FROM review_recheck WHERE photoid = ? AND proj = ?",
                    photoId, projId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
