package com.hodastar.photosreview.mappers;

import com.hodastar.photosreview.entities.EntityReviewUsers;
import com.hodastar.photosreview.utils.CryptUtil;
import com.hodastar.photosreview.utils.FileUtil;
import com.hodastar.photosreview.utils.Respond;
import com.hodastar.photosreview.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.*;

import static com.hodastar.photosreview.config.Config.LOGIN_SESSION_FILE_DIR;

@Repository
public class UserMapper {
    private final JdbcTemplate jdbcTemplate;

    private static final Logger log =
            LoggerFactory.getLogger(UserMapper.class);

    public UserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 通过用户 ID 获取用户信息
     * @param uid 用户 ID
     * @return 用户信息
     */
    public Optional<EntityReviewUsers> getUserByUid(int uid) {
        List<EntityReviewUsers> list = jdbcTemplate.query(
                "SELECT * FROM review_users WHERE uid = ?",
                (rs, rowNum) -> {
                    return new EntityReviewUsers(
                            rs.getInt("uid"),
                            rs.getString("password"),
                            rs.getLong("login_time"),
                            rs.getString("allname"),
                            rs.getInt("status")
                    );
                },
                uid);
        return list.stream().findFirst();
    }

    /**
     * 获取所有用户
     * @return 用户列表
     */
    public List<EntityReviewUsers> getUserList() {
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

    /**
     * 判断uid是否被占用
     * @param uid 用户 ID
     * @return 是否被占用
     */
    public Boolean isUidExist(int uid) {
        Boolean isExist = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM review_users WHERE uid = ?)",
                Boolean.class,
                uid
        );
        return Boolean.TRUE.equals(isExist);
    }

    /**
     * 更新登录时间
     * @param uid 用户 ID
     * @return 登录结果map
     */
    public Boolean updateLoginTime(int uid, long currentTime) {

        // 更新登录时间
        int rowsAffected = jdbcTemplate.update(
                "UPDATE review_users SET login_time = ? WHERE uid = ?",
                currentTime, uid
        );
        return rowsAffected != 0;
    }

    /**
     * 更新密码
     * @param uid 用户 ID
     * @param newPassword 新加密后的密码
     * @return 更新结果     */
    public Boolean updatePassword(int uid, String newPassword) {
        int rowsAffected = jdbcTemplate.update(
                "UPDATE review_users SET password = ? WHERE uid = ?",
                newPassword, uid
        );
        return rowsAffected != 0;
    }

    /**
     * 检查token
     * @param uid 用户 ID
     * @param token token
     * @return 是否有效
     */
    public Boolean checkToken(int uid, String token) {
        String dirStr = LOGIN_SESSION_FILE_DIR + String.valueOf(uid) + ".session";
        // 检查是否存在session文件
        File sessionFile = new File(dirStr);
        if (sessionFile.exists()) {
            // 验证session
            String sessionToken = FileUtil.readDocumentFile(dirStr);
            if (sessionToken != null && CryptUtil.nBCrypt2(token) == sessionToken) {
                return true;
            }
        }
        Optional<EntityReviewUsers> user = getUserByUid(uid);
        if (user.isEmpty()) {
            return false;
        }
        if (user.get().status == 2) {
            return false;
        }
        String originalToken = String.valueOf(uid) + String.valueOf(user.get().login_time);
        String dbToken = CryptUtil.nBCrypt(originalToken);
        if (!dbToken.equals(token)) {
            log.info("用户 {} token 无效", uid);
            return false;
        }

        // 重新存储token
        String sessionToken = CryptUtil.nBCrypt2(token);
        FileUtil.saveDocumentFile(sessionToken, LOGIN_SESSION_FILE_DIR, String.valueOf(uid) + ".session");
        return true;
    }

    /**
     * 检查管理员权限
     * @param adminUid 管理员用户 ID
     * @param adminToken 管理员token
     * @return 是否是管理员
     */
    public Boolean checkAdmin(int adminUid, String adminToken) {
        // 检查token
        if (!checkToken(adminUid, adminToken)) {
            return false;
        }

        // 检查管理员权限
        Optional<EntityReviewUsers> adminUser = getUserByUid(adminUid);
        if (adminUser.isEmpty()) {
            return false;
        }
        if (adminUser.get().status != 0) {
            return false;
        }
        return true;
    }

    /**
     * 注册
     * @param uid 用户 ID
     * @param allname 用户全名
     * @param status 用户状态(0=管理员, 1=普通用户)
     * @return 注册结果map
     */
    public Boolean register(int uid, String allname, int status) {
        // 获取当前秒级时间戳
        long currentTime = System.currentTimeMillis() / 1000;
        // 加密密码
        String ppassword = CryptUtil.BCEcrypt("Aa123456");

        // 插入新用户
        int rowsAffected = jdbcTemplate.update(
                "INSERT INTO review_users (uid, password, login_time, allname, status) VALUES (?, ?, ?, ?, ?)",
                uid, ppassword, currentTime, allname, status
        );
        return rowsAffected != 0;
    }

    /**
     * 更新用户状态
     * @param uid 用户 ID
     * @param status 用户状态
     * @return 更新结果
     */
    public Boolean updateStatus(int uid, int status) {
        int rowsAffected = jdbcTemplate.update(
                "UPDATE review_users SET status = ? WHERE uid = ?",
                status, uid
        );
        return rowsAffected != 0;
    }

    /**
     * 删除用户
     * @param uid 用户 ID
     * @return 删除结果
     */
    public Boolean deleteUser(int uid) {
        int rowsAffected = jdbcTemplate.update(
                "DELETE FROM review_users WHERE uid = ?",
                uid
        );
        return rowsAffected != 0;
    }

    // 重设10000的密码
    public Boolean resetAdminPassword() {
        // 生成密码
        String password = CryptUtil.BCEcrypt("Aa123456");
        // 修改
        int rowAffected = jdbcTemplate.update(
                "UPDATE review_users SET password = ? WHERE uid = 10000",
                password
        );
        return rowAffected != 0;
    }

    /**
     * 修改用户名
     * @param uid 用户ID
     * @param name 新用户名
     * @return 结果
     */
    public Boolean renameUser(int uid, String name) {
        // 修改
        return jdbcTemplate.update(
                "UPDATE review_users SET allname = ? WHERE uid = ?", name, uid
        ) != 0;
    }
}
