/*
 * HDSL-web
 * Copyright (C) 2026  HDSL contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.auth;

import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// 登录门真的会在预算花完之后关上：走真 HTTP，十六次错口令之后连对的口令也进不来。
///
/// 「连对的口令也挡住」是刻意钉在测试里的：拒绝发生在核对口令**之前**，而核对口令正是攻击者想花
/// 掉的那笔开销（PBKDF2 二十一万次迭代）。代价是误敲十六次的人要等十分钟——那是这个限制本身的
/// 意思，不是副作用。
class LoginThrottleApiTest {

    /// 管理员口令，由首启的环境变量建立。
    private static final String PASSWORD = "correct-horse-battery";

    @TempDir
    Path dataDir;

    @Test
    void theBudgetIsSpentAndThenTheDoorIsShut() throws Exception {
        try (TestSupport.RunningServer running = TestSupport.start(dataDir,
                Map.of(UserStore.ENV_ADMIN_PASSWORD, PASSWORD),
                config -> config.bindHost = "127.0.0.1")) {
            HttpClient client = TestSupport.client();
            String base = running.baseUrl();

            assertEquals(200, login(client, base, PASSWORD).statusCode(), "先证明这把钥匙是对的");

            for (int i = 1; i <= LoginThrottle.MAX_FAILURES; i++) {
                assertEquals(401, login(client, base, "wrong-" + i).statusCode(),
                        "第 " + i + " 次失败仍在预算内");
            }

            HttpResponse<String> refused = login(client, base, "wrong-again");
            assertEquals(429, refused.statusCode(), "第十六次失败被拒绝");
            assertEquals(String.valueOf(LoginThrottle.BLOCK.toSeconds()),
                    refused.headers().firstValue("Retry-After").orElse(""),
                    "并说明多久之后再来");

            assertEquals(429, login(client, base, PASSWORD).statusCode(),
                    "被拒绝期间，对的钥匙也一样进不来");
        }
    }

    /// 对一个口令发起一次登录。
    ///
    /// @param client   the client
    /// @param base     the panel's address
    /// @param password the password to try
    /// @return the response
    private static HttpResponse<String> login(HttpClient client, String base, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"admin\",\"password\":\"" + password + "\"}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}