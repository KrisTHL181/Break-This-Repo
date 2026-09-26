package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.entities.EntityReviewUsers;
import com.hodastar.photosreview.mappers.UserMapper;
import com.hodastar.photosreview.utils.CryptUtil;
import com.hodastar.photosreview.utils.FileUtil;
import com.hodastar.photosreview.utils.Respond;
import com.hodastar.photosreview.utils.Utilities;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.hodastar.photosreview.config.Config.LOGIN_SESSION_FILE_DIR;

@RestController
@RequestMapping("/api/user")
public class UserAPI {

    private final UserMapper userMapper;

    public UserAPI(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 注册接口
     * param uid uid
     * param allname 用户显示名
     * param status 用户状态(0=管理员, 1=普通用户)
     * @return 注册结果
     */
    @PostMapping("/register")
    public Respond<String> register(@RequestBody HashMap<String, Object> body) {
        // 检查类型
        if (!body.containsKey("uid") || !body.containsKey("allname") || !body.containsKey("status") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("allname") instanceof String) || !(body.get("status") instanceof Integer) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        int uid = (Integer) body.get("uid");
        String allname = ((String) body.get("allname")).trim();
        int status = (Integer) body.get("status");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 检查长度
        if (allname.isEmpty() || allname.length() > 10) {
            return new Respond<>(false, "6", null);
        }
        if (uid > 999999999 || uid < 10000) {
            return new Respond<>(false, "7", null);
        }
        if (status != 0 && status != 1) {
            return new Respond<>(false, "1", null);
        }

        // 检查token
        if (!userMapper.checkToken(adminUid, adminToken)) {
            return new Respond<>(false, "4", null);
        }

        // 检查管理员权限
        Optional<EntityReviewUsers> adminUser = userMapper.getUserByUid(adminUid);
        if (adminUser.isEmpty()) {
            return new Respond<>(false, "5", null);
        }
        if (adminUser.get().status != 0) {
            return new Respond<>(false, "5", null);
        }

        // 检查uid是否占用
        Boolean isExist = userMapper.isUidExist(uid);
        if (Boolean.TRUE.equals(isExist)) {
            return new Respond<>(false, "2", null);
        }

        // 注册
        Boolean result = userMapper.register(uid, allname, status);
        if (result) {
            return new Respond<>(true, "success", null);
        } else {
            return new Respond<>(false, "0", null);
        }
    }

    // 获取用户列表（管理员）
    @GetMapping("/get_user_list_admin")
    public Respond<List<HashMap<String, Object>>> getUserListAdmin(
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken
    ) {
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }

        List<HashMap<String, Object>> userList = userMapper.getUserList().stream()
                .map(user -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("uid", user.uid);
                    map.put("allname", user.allname);
                    map.put("status", user.status);
                    return map;
                }).toList();

        return new Respond<>(true, "success", userList);
    }

    // 重置
    @PostMapping("/reset_password")
    public Respond<String> resetPassword(@RequestBody HashMap<String, Object> body) {
        if (!body.containsKey("uid") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (uid == adminUid) {
            return new Respond<>(false, "5", null);
        }
        if (userMapper.getUserByUid(uid).isEmpty()) {
            return new Respond<>(false, "20", null);
        }
        String newPasswordHash = CryptUtil.BCEcrypt("123456");
        Boolean result = userMapper.updatePassword(uid, newPasswordHash);
        return result ? new Respond<>(true, "success", null) : new Respond<>(false, "0", null);
    }

    // 封禁
    @PostMapping("/ban_user")
    public Respond<String> banUser(@RequestBody HashMap<String, Object> body) {
        if (!body.containsKey("uid") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (uid == adminUid) {
            return new Respond<>(false, "5", null);
        }
        if (userMapper.getUserByUid(uid).isEmpty()) {
            return new Respond<>(false, "20", null);
        }

        String sessionFilePath = LOGIN_SESSION_FILE_DIR + uid + ".session";
        File sessionFile = new File(sessionFilePath);
        if (sessionFile.exists()) {
            sessionFile.delete();
        }
        Boolean result = userMapper.updateStatus(uid, 2);
        return result ? new Respond<>(true, "success", null) : new Respond<>(false, "0", null);
    }

    // 解封
    @PostMapping("/unban_user")
    public Respond<String> unbanUser(@RequestBody HashMap<String, Object> body) {
        if (!body.containsKey("uid") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (uid == adminUid) {
            return new Respond<>(false, "5", null);
        }
        if (userMapper.getUserByUid(uid).isEmpty()) {
            return new Respond<>(false, "20", null);
        }
        Boolean result = userMapper.updateStatus(uid, 1);
        return result ? new Respond<>(true, "success", null) : new Respond<>(false, "0", null);
    }

    // 管理员设置
    @PostMapping("/setAdmin")
    public Respond<String> setAdmin(@RequestBody HashMap<String, Object> body) {
        if (
                !body.containsKey("uid") ||
                !body.containsKey("type") ||
                !body.containsKey("adminUid") ||
                !body.containsKey("adminToken")
        ) {
            return new Respond<>(false, "1", null);
        }
        if (
                !(body.get("uid") instanceof Integer) ||
                !(body.get("type") instanceof Integer) ||
                !(body.get("adminUid") instanceof Integer) ||
                !(body.get("adminToken") instanceof String)
        ) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        int type = (Integer) body.get("type");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (uid == adminUid) {
            return new Respond<>(false, "5", null);
        }

        Optional<EntityReviewUsers> user = userMapper.getUserByUid(uid);
        if (user.isEmpty()) {
            return new Respond<>(false, "20", null);
        }
        if (user.get().status == 2) {
            return new Respond<>(false, "29", null);

        }

        Boolean result;
        if (type == 1) {
            // 撤去
            result = userMapper.updateStatus(uid, 1);
        } else if (type == 0) {
            // 设置
            result = userMapper.updateStatus(uid, 0);
        } else {
            return new Respond<>(false, "1", null);
        }

        if (!result) {
            return  new Respond<>(false, "0", null);
        }

        return new Respond<>(true, "success", null);
    }

    // 通过uid删除用户
    @PostMapping("/delete_user")
    public Respond<String> deleteUser(@RequestBody HashMap<String, Object> body) {
        if (!body.containsKey("uid") || !body.containsKey("adminUid") || !body.containsKey("adminToken")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String)) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        if (uid == adminUid) {
            return new Respond<>(false, "5", null);
        }
        if (uid == 10000) {
            return new Respond<>(false, "5", null);
        }
        if (userMapper.getUserByUid(uid).isEmpty()) {
            return new Respond<>(false, "20", null);
        }

        String sessionFilePath = LOGIN_SESSION_FILE_DIR + uid + ".session";
        File sessionFile = new File(sessionFilePath);
        if (sessionFile.exists()) {
            sessionFile.delete();
        }
        Boolean result = userMapper.deleteUser(uid);
        return result ? new Respond<>(true, "success", null) : new Respond<>(false, "0", null);
    }

    /**
     * 登录接口
     * param uid uid
     * param password 密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public Respond<String> login(@RequestBody HashMap<String, Object> body) {
        // 检查类型
        if (!body.containsKey("uid") || !body.containsKey("password")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("password") instanceof String)) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        String password = (String) body.get("password");

        // 获取用户
        Optional<EntityReviewUsers> user = userMapper.getUserByUid(uid);
        if (user.isEmpty()) {
            return new Respond<>(false, "2", null);
        }
        // 验证密码
        if (!CryptUtil.checkBCEcrypt(password, user.get().password)) {
            return new Respond<>(false, "2", null);
        }
        // 检查封禁
        if (user.get().status == 2) {
            return new Respond<>(false, "29", null);
        }
        // 更新登录时间
        long currentTime = System.currentTimeMillis() / 1000;
        Boolean result = userMapper.updateLoginTime(uid, currentTime);
        if (!result) {
            return new Respond<>(false, "0", null);
        }

        // 合成token
        String originalToken = String.valueOf(uid) + String.valueOf(currentTime);
        String token = CryptUtil.nBCrypt(originalToken);
        // 存储token
        String sessionToken = CryptUtil.nBCrypt2(token);
        FileUtil.saveDocumentFile(sessionToken, LOGIN_SESSION_FILE_DIR, String.valueOf(uid) + ".session");
        // 返回token
        return new Respond<>(true, "success", token);
    }

    /**
     * 修改密码接口
     * param uid
     * param password
     * param newPassword
     * @return 修改结果
     */
    @PostMapping("/alter_password")
    public Respond<String> alterPassword(@RequestBody HashMap<String, Object> body) {
        // 检查类型
        if (!body.containsKey("uid") || !body.containsKey("password") || !body.containsKey("newPassword")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("uid") instanceof Integer) || !(body.get("password") instanceof String) || !(body.get("newPassword") instanceof String)) {
            return new Respond<>(false, "1", null);
        }
        int uid = (Integer) body.get("uid");
        String password = (String) body.get("password");
        String newPassword = (String) body.get("newPassword");

        // 检查长度
        if (password.length() > 255 || newPassword.length() > 255) {
            return new Respond<>(false, "6", null);
        }

        // 获取用户
        Optional<EntityReviewUsers> user = userMapper.getUserByUid(uid);
        if (user.isEmpty()) {
            return new Respond<>(false, "2", null);
        }
        // 验证密码
        if (!CryptUtil.checkBCEcrypt(password, user.get().password)) {
            return new Respond<>(false, "2", null);
        }
        // 加密密码
        String newPasswordHash = CryptUtil.BCEcrypt(newPassword);
        // 更新密码
        Boolean result = userMapper.updatePassword(uid, newPasswordHash);
        if (result) {
            return new Respond<>(true, "success", null);
        } else {
            return new Respond<>(false, "0", null);
        }
    }

    // 修改用户名接口
    @PostMapping("/rename")
    public Respond<String> rename(@RequestBody HashMap<String, Object> body) {
        // 检查类型
        if (!body.containsKey("adminUid") || !body.containsKey("adminToken") || !body.containsKey("uid") || !body.containsKey("name")) {
            return new Respond<>(false, "1", null);
        }
        if (!(body.get("adminUid") instanceof Integer) || !(body.get("adminToken") instanceof String) || !(body.get("uid") instanceof Integer) || !(body.get("name") instanceof String)) {
            return new Respond<>(false, "1", null);
        }

        int uid = (Integer) body.get("uid");
        String name = (String) body.get("name");
        int adminUid = (Integer) body.get("adminUid");
        String adminToken = (String) body.get("adminToken");

        // 检查权限
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            return new Respond<>(false, "5", null);
        }
        // 检查用户
        Optional<EntityReviewUsers> user = userMapper.getUserByUid(uid);
        if (user.isEmpty()) {
            return new Respond<>(false, "2", null);
        }

        // 修改
        Boolean result = userMapper.renameUser(uid, name);
        return new Respond<>(result, "Done", null);
    }

    // 查询token接口
    @GetMapping("/check_token")
    public Respond<Boolean> checkToken(@RequestParam int uid, @RequestParam String token) {
        Boolean result = userMapper.checkToken(uid, token);
        return new Respond<>(result, "Done", null);
    }

    // 获取用户信息
    @GetMapping("/get_user_info")
    public Respond<HashMap<String, Object>> getUserInfo(@RequestParam int uid, @RequestParam String token) {
        // 验证token
        if (!userMapper.checkToken(uid, token)) {
            return new Respond<>(false, "4", null);
        }
        Optional<EntityReviewUsers> user = userMapper.getUserByUid(uid);
        if (user.isEmpty()) {
            return new Respond<>(false, "2", null);
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("uid", user.get().uid);
        data.put("allname", user.get().allname);
        data.put("status", user.get().status);
        return new Respond<>(true, "success", data);
    }
}
