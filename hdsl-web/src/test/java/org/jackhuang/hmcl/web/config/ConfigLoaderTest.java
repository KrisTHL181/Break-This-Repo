/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
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
package org.jackhuang.hmcl.web.config;

import org.jackhuang.hmcl.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the declared configuration contract: defaults when nothing is
/// configured, every yaml field when it is, environment precedence over the
/// file, and loud failure (with the line number) on a broken file.
class ConfigLoaderTest {

    @TempDir
    Path dataDir;

    private ServerConfig load(Map<String, String> env) {
        return ServerConfigLoader.load(dataDir, env, Logger.LOG);
    }

    @Test
    void defaultsWhenNothingIsConfigured() {
        ServerConfig config = load(Map.of());

        assertEquals(Path.of("./data").toAbsolutePath().normalize().toString(),
                ServerConfigLoader.load(Map.of(), Logger.LOG).dataDir.toString(),
                "HDSL_DATA defaults to ./data");
        assertEquals("0.0.0.0", config.bindHost);
        assertEquals(3080, config.port);
        assertFalse(config.https.enabled);
        assertFalse(config.https.redirectHttp);
        assertEquals(80, config.https.redirectPort);
        assertTrue(config.https.certFile.isBlank() && config.https.keyFile.isBlank() && config.https.pkcs12File.isBlank());
        assertEquals("", config.publicBaseUrl);
        assertFalse(config.auth.disabled);
        assertEquals(168, config.auth.sessionTtlHours);
    }

    @Test
    void plainSsrWorkersDefaultToFourAndAreBounded() throws Exception {
        assertEquals(4, load(Map.of()).plain.ssrWorkers);

        Files.writeString(dataDir.resolve("server.yaml"), """
                plain:
                  ssr_workers: 6
                """);
        assertEquals(6, load(Map.of()).plain.ssrWorkers);

        Files.writeString(dataDir.resolve("server.yaml"), """
                plain:
                  ssr_workers: 17
                """);
        assertThrows(ServerConfigLoader.ConfigException.class, () -> load(Map.of()));
    }

    /// 非回环票的开关：yaml 能读、环境变量能覆盖，默认关。
    @Test
    void theAgentRemoteSessionSwitchIsRead() throws Exception {
        assertFalse(load(Map.of()).agent.remoteNeedsSession, "default stays off");

        Files.writeString(dataDir.resolve("server.yaml"), """
                agent:
                  remote_needs_session: true
                """);
        assertTrue(load(Map.of()).agent.remoteNeedsSession);

        Files.writeString(dataDir.resolve("server.yaml"), "");
        assertTrue(load(Map.of(ServerConfigLoader.ENV_AGENT_REMOTE_NEEDS_SESSION, "true"))
                .agent.remoteNeedsSession);
        assertFalse(load(Map.of(ServerConfigLoader.ENV_AGENT_REMOTE_NEEDS_SESSION, "false"))
                .agent.remoteNeedsSession);
    }

    /// 票的来源：默认只认回环；yaml 与环境变量都能放开。
    @Test
    void ticketsAreLoopbackOnlyByDefault() throws Exception {
        assertTrue(load(Map.of()).agent.loopbackOnly, "an agent runs beside the panel by default");

        Files.writeString(dataDir.resolve("server.yaml"), """
                agent:
                  loopback_only: false
                """);
        assertFalse(load(Map.of()).agent.loopbackOnly);

        Files.writeString(dataDir.resolve("server.yaml"), "");
        assertFalse(load(Map.of(ServerConfigLoader.ENV_AGENT_LOOPBACK_ONLY, "false"))
                .agent.loopbackOnly);
    }

    @Test
    void everyYamlFieldIsRead() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                bind_host: 127.0.0.1
                port: 12345
                public_base_url: https://dsh.example.com
                https:
                  enabled: true
                  cert_file: certs/fullchain.pem
                  key_file: certs/privkey.pem
                  pkcs12_file: certs/server.p12
                  pkcs12_password: "secret"
                  redirect_http: true
                  redirect_port: 8080
                auth:
                  disabled: false
                  session_ttl_hours: 24
                """);

        ServerConfig config = load(Map.of());

        assertEquals("127.0.0.1", config.bindHost);
        assertEquals(12345, config.port);
        assertEquals("https://dsh.example.com", config.publicBaseUrl);
        assertTrue(config.https.enabled);
        assertEquals("certs/fullchain.pem", config.https.certFile);
        assertEquals("certs/privkey.pem", config.https.keyFile);
        assertEquals("certs/server.p12", config.https.pkcs12File);
        assertEquals("secret", config.https.pkcs12Password);
        assertTrue(config.https.redirectHttp);
        assertEquals(8080, config.https.redirectPort);
        assertFalse(config.auth.disabled);
        assertEquals(24, config.auth.sessionTtlHours);
    }

    @Test
    void environmentBeatsTheFile() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                bind_host: 0.0.0.0
                port: 4000
                https:
                  enabled: false
                """);

        ServerConfig config = load(Map.of(
                ServerConfigLoader.ENV_PORT, "9999",
                ServerConfigLoader.ENV_BIND_HOST, "127.0.0.1",
                ServerConfigLoader.ENV_HTTPS, "true"));

        assertEquals(9999, config.port);
        assertEquals("127.0.0.1", config.bindHost);
        assertTrue(config.https.enabled);
    }

    @Test
    void hdslHttpsRejectsAnythingButTrueOrFalse() {
        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class,
                () -> load(Map.of(ServerConfigLoader.ENV_HTTPS, "yes")));
        assertTrue(e.getMessage().contains("HDSL_HTTPS"), e.getMessage());
    }

    @Test
    void brokenYamlReportsTheLine() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                bind_host: 0.0.0.0
                port: [1, 2
                """);

        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class, () -> load(Map.of()));
        assertTrue(e.getMessage().contains("line 2"), "the message must name the line: " + e.getMessage());
    }

    @Test
    void wrongYamlTypeNamesTheField() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                port: not-a-number
                """);

        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class, () -> load(Map.of()));
        assertTrue(e.getMessage().contains("port"), e.getMessage());
    }

    @Test
    void authDisabledNeedsALoopbackBind() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                bind_host: 0.0.0.0
                auth:
                  disabled: true
                """);

        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class, () -> load(Map.of()));
        assertTrue(e.getMessage().contains("auth.disabled"), e.getMessage());
    }

    @Test
    void authDisabledIsAcceptedOnLoopback() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                bind_host: 127.0.0.1
                auth:
                  disabled: true
                """);

        ServerConfig config = load(Map.of());
        assertTrue(config.auth.disabled);
    }

    @Test
    void redirectHttpRequiresHttps() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                https:
                  redirect_http: true
                """);

        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class, () -> load(Map.of()));
        assertTrue(e.getMessage().contains("redirect_http"), e.getMessage());
    }

    @Test
    void halfConfiguredPemIsAnError() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                https:
                  enabled: true
                  cert_file: certs/fullchain.pem
                """);

        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class, () -> load(Map.of()));
        assertTrue(e.getMessage().contains("cert_file"), e.getMessage());
    }

    @Test
    void theDeveloperGuardIsOffUnlessAskedFor() {
        // The default is the whole point: an upgrade must not start gating
        // anything that used to be open.
        assertEquals(DeveloperGuardScope.OFF, load(Map.of()).developerGuard.scope);
    }

    @Test
    void theDeveloperGuardScopeIsReadFromYamlAndEnvironment() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                developer_guard:
                  scope: instance
                """);
        assertEquals(DeveloperGuardScope.INSTANCE, load(Map.of()).developerGuard.scope);

        // The environment wins over the file, the same way every other knob does.
        Map<String, String> env = Map.of(ServerConfigLoader.ENV_DEVELOPER_GUARD, "all");
        assertEquals(DeveloperGuardScope.ALL, load(env).developerGuard.scope);
    }

    @Test
    void anUnknownDeveloperGuardScopeIsAnError() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                developer_guard:
                  scope: sometimes
                """);
        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class,
                () -> load(Map.of()));
        assertTrue(e.getMessage().contains("developer_guard.scope"), e.getMessage());
        assertTrue(e.getMessage().contains("sometimes"), e.getMessage());
    }

    @Test
    void anUnknownKeyInsideTheGuardSectionIsAnError() throws Exception {
        // A typo like `scopes:` would otherwise leave the guard off while the
        // operator believes it is on.
        Files.writeString(dataDir.resolve("server.yaml"), """
                developer_guard:
                  scopes: all
                """);
        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class,
                () -> load(Map.of()));
        assertTrue(e.getMessage().contains("scopes"), e.getMessage());
    }

    @Test
    void transcodeSettingsAreReadFromYamlAndEnvironment() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                transcode:
                  media_root: /srv/video
                  ffmpeg_path: /opt/ffmpeg/bin/ffmpeg
                  max_concurrent: 2
                  max_queue: 50
                  failure_policy: timeout-auto
                  failure_timeout_minutes: 15
                  auto_resume: false
                """);

        ServerConfig config = load(Map.of());
        assertEquals("/srv/video", config.transcode.mediaRoot);
        assertEquals("/opt/ffmpeg/bin/ffmpeg", config.transcode.ffmpegPath);
        assertEquals(2, config.transcode.maxConcurrent);
        assertEquals(50, config.transcode.maxQueue);
        assertEquals("timeout-auto", config.transcode.failurePolicy);
        assertEquals(15, config.transcode.failureTimeoutMinutes);
        assertFalse(config.transcode.autoResume);

        Map<String, String> env = Map.of(
                ServerConfigLoader.ENV_MEDIA_ROOT, "/mnt/media",
                ServerConfigLoader.ENV_TRANSCODE_MAX_CONCURRENT, "1",
                ServerConfigLoader.ENV_TRANSCODE_FAILURE_POLICY, "full-auto");
        ServerConfig overridden = load(env);
        assertEquals("/mnt/media", overridden.transcode.mediaRoot);
        assertEquals(1, overridden.transcode.maxConcurrent);
        assertEquals("full-auto", overridden.transcode.failurePolicy);
    }

    @Test
    void anUnknownTranscodeFailurePolicyIsAnError() throws Exception {
        Files.writeString(dataDir.resolve("server.yaml"), """
                transcode:
                  failure_policy: sometimes
                """);
        ServerConfigLoader.ConfigException e = assertThrows(ServerConfigLoader.ConfigException.class,
                () -> load(Map.of()));
        assertTrue(e.getMessage().contains("transcode.failure_policy"), e.getMessage());
    }
    @Test
    void theTranscodeFailurePolicyCanBePersisted() throws Exception {
        ServerConfigWriter.setTranscodeFailurePolicy(dataDir, "full-auto", 7);
        ServerConfig reloaded = load(Map.of());
        assertEquals("full-auto", reloaded.transcode.failurePolicy);
        assertEquals(7, reloaded.transcode.failureTimeoutMinutes);
    }

}
