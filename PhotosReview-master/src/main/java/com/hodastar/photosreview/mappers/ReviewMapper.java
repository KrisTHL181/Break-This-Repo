package com.hodastar.photosreview.mappers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewRecheck;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class ReviewMapper {
    private final JdbcTemplate jdbcTemplate;

    public ReviewMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    // 获取全部照片列表
    public List<EntityReviewPhotos> getAllPhotos(
            String projId,
            String author
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM review_data WHERE 1=1");
        List<Object> params = new ArrayList<>();

        sql.append(" AND proj = ?");
        params.add(projId);

        if (author != null) {
            sql.append(" AND author LIKE ?");
            params.add("%" + author + "%");
        }

        sql.append(" ORDER BY id ASC");

        return jdbcTemplate.query(
                 sql.toString(),
                (rs, rowNum) -> {
                    return new EntityReviewPhotos(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("proj"),
                            rs.getString("author"),
                            rs.getString("value")
                    );
                },
                params.toArray()
        );
    }

    // 获取工程全部照片文件名
    public List<String> getAllPhotosName(
            String projId
    ) {
        return jdbcTemplate.query(
                "SELECT * FROM review_data WHERE proj = ? ORDER BY id ASC",
                (rs, rowNum) -> rs.getString("name"),
                projId
        );
    }

    // 根据工程id、闭区间获取照片列表
    public List<EntityReviewPhotos> getPhotos(
            String projId,
            int start,
            int end
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM review_data WHERE 1=1");
        List<Object> params = new ArrayList<>();
        int num = end - start + 1;

        sql.append(" AND proj = ?");
        params.add(projId);

        sql.append(" ORDER BY id ASC");

        sql.append(" LIMIT ?");
        params.add(num);

        sql.append(" OFFSET ?");
        params.add(start);

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> {
                    return new EntityReviewPhotos(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("proj"),
                            rs.getString("author"),
                            rs.getString("value")
                    );
                },
                params.toArray()
        );
    }

    // 获取复审照片列表
    public List<EntityReviewRecheck> getRecheckPhotos(String projId) {
        return jdbcTemplate.query(
                "SELECT * FROM review_recheck WHERE proj = ? ORDER BY photoid ASC",
                (rs, rowNum) -> new EntityReviewRecheck(
                            rs.getInt("photoid"),
                            rs.getString("proj"),
                            rs.getString("value"),
                            rs.getDouble("final_score")
                    ),
                projId
        );
    }

    // 通过photoid查询图片
    public Optional<EntityReviewPhotos> getPhotoById(int photoId) {
        List<EntityReviewPhotos> list = jdbcTemplate.query(
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
                photoId
        );
        return list.stream().findFirst();
    }

    // 通过photoid查询复审图片
    public Optional<EntityReviewRecheck> getRecheckPhotoById(int photoId) {
        List<EntityReviewRecheck> list = jdbcTemplate.query(
                "SELECT * FROM review_recheck WHERE photoid = ?",
                (rs, rowNum) -> {
                    return new EntityReviewRecheck(
                            rs.getInt("photoid"),
                            rs.getString("proj"),
                            rs.getString("value"),
                            rs.getDouble("final_score")
                    );
                },
                photoId
        );
        return list.stream().findFirst();
    }

    // 通过多个同工程的photoid查询图片名
    public List<String> getPhotoNamesByIds(List<Integer> ids, String proj) {
        if (ids.isEmpty() || proj.isEmpty()) {
            return new ArrayList<>();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = """
                SELECT * FROM review_data WHERE id IN (%s) AND proj = ?
                """.formatted(placeholders);
        List<Object> params = new ArrayList<>(ids);
        params.add(proj);

        return jdbcTemplate.query(
                sql,
                params.toArray(),
                (rs, rowNum) -> rs.getString("name")
        );
    }

    // 写入评分
    public Boolean updatePhotoValue(int photoId, String value) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "UPDATE review_data SET value = ? WHERE id = ?",
                    value, photoId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 写入复审评分
    public Boolean updateRecheckValue(int photoId, String value) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "UPDATE review_recheck SET value = ? WHERE photoid = ?",
                    value, photoId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 写入最终评分
    public Boolean updateFinalScore(int photoId, double finalScore) {
        try {
            int rowsAffected = jdbcTemplate.update(
                    "UPDATE review_recheck SET final_score = ? WHERE photoid = ?",
                    finalScore, photoId
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
