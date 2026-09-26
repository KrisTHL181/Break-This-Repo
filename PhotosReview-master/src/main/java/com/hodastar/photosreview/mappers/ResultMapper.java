package com.hodastar.photosreview.mappers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.utils.Utilities;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ResultMapper {
    private final JdbcTemplate jdbcTemplate;

    public ResultMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 添加或覆盖结果数据
    public Boolean insertResult(String proj, String value)
    {
        try {
            jdbcTemplate.update(
                "INSERT INTO review_result (id, proj, value, time) VALUES (NULL, ?, ?, ?) ON CONFLICT (proj) DO UPDATE SET value = EXCLUDED.value, time = EXCLUDED.time",
                proj, value, Utilities.nowTimeString()
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 获取结果数据
    public String getResultByProjId(String proj) {
        try {
            String result = jdbcTemplate.queryForObject(
                    "SELECT value FROM review_result WHERE proj = ?",
                    String.class,
                    proj
            );
            return result != null ? result : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 根据作者获取初审数据
    public List<EntityReviewPhotos> getPhotosByAuthor(String projId, String author) {
        String sql = "SELECT * FROM review_data WHERE proj = ? AND author = ? ORDER BY id ASC";
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new EntityReviewPhotos(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("proj"),
                        rs.getString("author"),
                        rs.getString("value")
                ),
                projId, author
        );
    }
}
