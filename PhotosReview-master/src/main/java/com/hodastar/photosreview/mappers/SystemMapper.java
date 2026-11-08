package com.hodastar.photosreview.mappers;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class SystemMapper {
    private final JdbcTemplate jdbcTemplate;

    public  SystemMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取网站名称
     * @return 网站名称
     */
    public String getWebsiteName() {
        String name = jdbcTemplate.queryForObject(
            "SELECT value FROM review_config WHERE id = 1",
            String.class
        );
        return name;
    }

    /**
     * 获取网站图标路径
     * @return 网站图标路径
     */
    public String getWebsiteIcon() {
        String icon = jdbcTemplate.queryForObject(
                "SELECT value FROM review_config WHERE id = 2",
                String.class
        );
        return icon;
    }

    public Boolean updateWebsiteName(String websiteName) {
        int updated = jdbcTemplate.update(
                "UPDATE review_config SET value = ? WHERE id = 1",
                websiteName
        );
        return updated > 0;
    }

    public Boolean updateWebsiteIcon(String websiteIcon) {
        int updated = jdbcTemplate.update(
                "UPDATE review_config SET value = ? WHERE id = 2",
                websiteIcon
        );
        return updated > 0;
    }
}
