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
package org.jackhuang.hmcl.web.server;

import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.OptionalSslConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.acp.AcpSessionManager;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.http.AccountsApiServlet;
import org.jackhuang.hmcl.web.http.ApiGuardFilter;
import org.jackhuang.hmcl.web.http.ApiServlet;
import org.jackhuang.hmcl.web.http.DoctorApiServlet;
import org.jackhuang.hmcl.web.http.DshApiServlet;
import org.jackhuang.hmcl.web.http.ExportsApiServlet;
import org.jackhuang.hmcl.web.http.InstancesApiServlet;
import org.jackhuang.hmcl.web.http.NodeApiServlet;
import org.jackhuang.hmcl.web.http.ModelsApiServlet;
import org.jackhuang.hmcl.web.http.BrandSettingsApiServlet;
import org.jackhuang.hmcl.web.http.PacksApiServlet;
import org.jackhuang.hmcl.web.http.PlainSettingsApiServlet;
import org.jackhuang.hmcl.web.http.PluginsApiServlet;
import org.jackhuang.hmcl.web.http.LogRetentionApiServlet;
import org.jackhuang.hmcl.web.http.RegistrySettingsApiServlet;
import org.jackhuang.hmcl.web.http.StaticServlet;
import org.jackhuang.hmcl.web.http.TasksApiServlet;
import org.jackhuang.hmcl.web.http.TlsApiServlet;
import org.jackhuang.hmcl.web.http.VendorsApiServlet;
import org.jackhuang.hmcl.web.http.VersionsApiServlet;
import org.jackhuang.hmcl.web.instance.InstanceRuntime;
import org.jackhuang.hmcl.web.proxy.InstanceProxyServlet;
import org.jackhuang.hmcl.web.proxy.InstanceProxyWebSocket;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jackhuang.hmcl.web.tls.CertificateManager;
import org.jackhuang.hmcl.web.ws.WsGateway;
import org.jackhuang.hmcl.web.brand.BrandApiServlet;
import org.jackhuang.hmcl.web.brand.BrandCatalog;
import org.jackhuang.hmcl.web.chat.ChatApiServlet;
import org.jackhuang.hmcl.web.chat.ChatModule;
import org.jackhuang.hmcl.web.plain.MarkdownSsr;
import org.jackhuang.hmcl.web.plain.PlainChatServlet;
import org.jackhuang.hmcl.web.zcode.ZcodeApiServlet;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/// The Jetty assembly behind the Phase 1 server:
///
/// - the main connector on `bind_host:port`, speaking TLS when
///   `https.enabled` (identity from [CertificateManager]); with
///   `https.redirect_same_port` (on by default) that same port also answers
///   cleartext requests with a 308 to HTTPS, so a bare `http://<ip>` entry
///   reaches the panel instead of `ERR_INVALID_HTTP_RESPONSE`;
/// - when `https.redirect_http` is on, a second plain-HTTP connector that
///   answers every request with 308 to the same host/path over HTTPS;
/// - one servlet context holding the session gate ([AuthFilter] on `/api/*`,
///   `/i/*` and `/ws/*`), the REST surface (auth/health/instances/versions/
///   tasks), the reverse proxy into dsh instances under `/i/*` (HTTP in
///   [org.jackhuang.hmcl.web.proxy.InstanceProxyServlet], WebSocket upgrades
///   relayed through [org.jackhuang.hmcl.web.proxy.InstanceProxyWebSocket]),
///   the `/ws` event gateway ([org.jackhuang.hmcl.web.ws.WsGateway]) and the
///   SPA shell ([StaticServlet]);
/// - the Phase 2 services behind it: [EventBus], [TaskService] and
///   [InstanceRuntime].
///
/// [start] registers a JVM shutdown hook so SIGTERM stops Jetty, whose stop
/// sequence drains in-flight connections (default 30s stop timeout) — a
/// shutdown does not cut requests in half.
@NotNullByDefault
public final class HdslServer {

    private final Server server;
    private final HttpConfiguration baseHttpConfig;
    /// The connector serving the panel — replaced wholesale when the
    /// certificate upload switches the port between plain HTTP and TLS at
    /// runtime. volatile: request threads read it while a switch is in flight.
    private volatile ServerConnector mainConnector;
    private final @Nullable ServerConnector redirectConnector;
    private volatile @Nullable SslContextFactory.Server sslContextFactory;
    private final WebSocketClient proxyWebSocketClient;
    /// The ACP console bridge; its processes outlive `/ws` connections and are
    /// all stopped when the server stops.
    private final AcpSessionManager acpSessions;
    /// The web terminal's sessions (docs/web-shell.md), the only way into a
    /// TUI from the panel.
    private final org.jackhuang.hmcl.web.terminal.TerminalManager terminals;
    private final org.jackhuang.hmcl.web.minecraft.McRuntime mcRuntime;
    /// 面板聊天（docs/chat.md）：SQLite 会话库、turn 管理器与备份调度。
    /// `null` when the store could not be opened (an unsupported platform or a
    /// missing native library). Chat is the only feature whose storage is a
    /// native dependency; a panel that cannot chat must still boot.
    private final @Nullable ChatModule chat;
    /// The machine's own readings (docs/web-shell.md §通用 → 系统资源). One
    /// sampler for every viewer; it runs in two gears and never faster than a
    /// second.
    private final org.jackhuang.hmcl.web.system.SystemMetrics systemMetrics;
    private final Logger logger;
    private final AtomicLong startedAtMillis;
    private final AtomicBoolean hookInstalled = new AtomicBoolean();

    private HdslServer(Server server, HttpConfiguration baseHttpConfig, ServerConnector mainConnector,
                       @Nullable ServerConnector redirectConnector,
                       @Nullable SslContextFactory.Server sslContextFactory, WebSocketClient proxyWebSocketClient,
                       AcpSessionManager acpSessions,
                       org.jackhuang.hmcl.web.terminal.TerminalManager terminals,
                       org.jackhuang.hmcl.web.minecraft.McRuntime mcRuntime,
                       org.jackhuang.hmcl.web.system.SystemMetrics systemMetrics,
                       @Nullable ChatModule chat,
                       Logger logger, AtomicLong startedAtMillis) {
        this.server = server;
        this.baseHttpConfig = baseHttpConfig;
        this.mainConnector = mainConnector;
        this.redirectConnector = redirectConnector;
        this.sslContextFactory = sslContextFactory;
        this.proxyWebSocketClient = proxyWebSocketClient;
        this.acpSessions = acpSessions;
        this.terminals = terminals;
        this.mcRuntime = mcRuntime;
        this.systemMetrics = systemMetrics;
        this.chat = chat;
        this.logger = logger;
        this.startedAtMillis = startedAtMillis;
    }

    /// Assembles the server for `config`; nothing binds until [start].
    ///
    /// @param certificates the TLS identity source; may be null when HTTPS is
    ///                     off (then the config must say so too)
    public static HdslServer build(ServerConfig config, AuthService authService,
                                   @Nullable CertificateManager certificates, Logger logger) {
        AuditLog.init(config.dataDir.resolve("logs"));
        Server server = new Server();
        // The stop sequence is owned by our own shutdown hook (below); Jetty's
        // built-in one would stop the server twice without adding anything.
        server.setStopAtShutdown(false);

        // Uptime is reported from the moment start() returned; the servlet is
        // built first, so it reads this holder.
        AtomicLong startedAtMillis = new AtomicLong();

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);

        SslContextFactory.Server ssl = null;
        ServerConnector mainConnector;
        if (config.https.enabled) {
            if (certificates == null) {
                throw new IllegalArgumentException("HTTPS is enabled but no CertificateManager was provided");
            }
            ssl = new SslContextFactory.Server();
            certificates.applyTo(ssl);
            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.setSecureScheme("https");
            httpsConfig.setSecurePort(config.port);
            httpsConfig.addCustomizer(secureRequestCustomizer());
            if (config.https.redirectSamePort) {
                // Sniff the first bytes: TLS stays TLS, and cleartext on this same
                // port is answered by the redirect handler — never by panel
                // content. Without this, a bare `http://<ip>` entry (or a host
                // port 80 mapped onto this port) is `ERR_INVALID_HTTP_RESPONSE`.
                SslConnectionFactory sslFactory = new SslConnectionFactory(ssl, "http/1.1");
                mainConnector = new ServerConnector(server,
                        new OptionalSslConnectionFactory(sslFactory, "http/1.1"), sslFactory,
                        new HttpConnectionFactory(httpsConfig));
            } else {
                mainConnector = new ServerConnector(server, ssl, new HttpConnectionFactory(httpsConfig));
            }
        } else {
            mainConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        }
        mainConnector.setHost(config.bindHost);
        mainConnector.setPort(config.port);
        server.addConnector(mainConnector);

        ServerConnector redirectConnector = null;
        if (config.https.enabled && config.https.redirectHttp) {
            redirectConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            redirectConnector.setHost(config.bindHost);
            redirectConnector.setPort(config.https.redirectPort);
            server.addConnector(redirectConnector);
        }

        int ttlSeconds = Math.toIntExact(java.time.Duration.ofHours(config.auth.sessionTtlHours).toSeconds());
        // The elevation registry is created before the session gate: an agent
        // reaches `/api/*` with a bearer token rather than a cookie, so the
        // gate has to know the grants to admit one.
        org.jackhuang.hmcl.web.developer.DeveloperElevation developerElevation =
                new org.jackhuang.hmcl.web.developer.DeveloperElevation();
        // The web terminal's manager, hoisted above the session gate: the gate
        // derives which instance a caller runs in from the terminal roots and
        // the web-process roots, so it needs the manager before the first
        // request, not after the first terminal.
        org.jackhuang.hmcl.web.terminal.TerminalManager terminals =
                new org.jackhuang.hmcl.web.terminal.TerminalManager();
        org.jackhuang.hmcl.web.auth.PeerIdentity peerIdentity =
                new org.jackhuang.hmcl.web.auth.PeerIdentity(terminals);
        AuthFilter authFilter = new AuthFilter(authService, config.auth.disabled,
                request -> request.isSecure()
                        || org.jackhuang.hmcl.web.http.RequestSecurity.forwardedHttps(request, config),
                ttlSeconds, config.basePath, developerElevation, peerIdentity,
                config.agent.remoteNeedsSession, config.agent.loopbackOnly);

        // The Phase 2 services: the event bus every WebSocket payload flows
        // through, the task pool behind installs and launches, and the
        // runtime that turns domain events into the panel's event stream.
        EventBus eventBus = new EventBus();
        TaskService taskService = new TaskService(eventBus);
        // The emergency stop's state: one object, in memory, consulted by the
        // proxy gates, the terminal and the instance launcher (docs/web-shell.md).
        // Created before the runtime because launching an instance is one of the
        // things it freezes.
        org.jackhuang.hmcl.web.emergency.EmergencyStop emergencyStop =
                new org.jackhuang.hmcl.web.emergency.EmergencyStop();
        InstanceRuntime runtime = new InstanceRuntime(eventBus, taskService, emergencyStop);
        runtime.wire();
        // The last-resort fixed doors survive a panel restart: the policy file
        // is the source of truth, the runtime only caches each brand's survivor.
        new org.jackhuang.hmcl.web.brand.BrandPolicy(config.dataDir).lockedSurvivors()
                .forEach(org.jackhuang.hmcl.web.brand.BrandRuntime::setLockedSurvivor);
        AcpSessionManager acpSessions = new AcpSessionManager(eventBus);
        WsGateway.install(eventBus, authService, config.auth.disabled, acpSessions);

        // The web terminal (docs/web-shell.md): the agent TUI runs on a PTY
        // inside the panel; the browser is only a viewer. Tickets bridge the
        // authenticated POST and the WebSocket handshake, and the manager owns
        // the one-terminal-per-instance rule. The manager itself lives above,
        // beside the session gate that identifies callers by it.
        org.jackhuang.hmcl.web.terminal.TerminalTickets terminalTickets =
                new org.jackhuang.hmcl.web.terminal.TerminalTickets();
        org.jackhuang.hmcl.web.terminal.TerminalWebSocket.configure(
                terminals, terminalTickets, authService, config.auth.disabled, developerElevation);

        // The machine's own readings (通用 → 系统资源). One sampler for every
        // viewer, two gears, never faster than a second; it starts slow and
        // switches to the fast gear when a request touches SystemActivity.
        org.jackhuang.hmcl.web.system.SystemMetrics systemMetrics =
                new org.jackhuang.hmcl.web.system.SystemMetrics(config.dataDir);
        systemMetrics.start();
        org.jackhuang.hmcl.web.system.SystemActivity.install(systemMetrics::touch);

        // The client behind the `/i/*` WebSocket relay; its life is the
        // server's own. Its message limits are raised for the same reason the
        // server-side ones are (see InstanceProxyWebSocket#MAX_MESSAGE_BYTES):
        // Jetty's 64 KiB default fails the connection on one session history.
        WebSocketClient proxyWebSocketClient = new WebSocketClient();
        proxyWebSocketClient.setMaxFrameSize(InstanceProxyWebSocket.MAX_MESSAGE_BYTES);
        proxyWebSocketClient.setMaxTextMessageSize(InstanceProxyWebSocket.MAX_MESSAGE_BYTES);
        proxyWebSocketClient.setMaxBinaryMessageSize(InstanceProxyWebSocket.MAX_MESSAGE_BYTES);

        // Multipart uploads (a plugin .tgz, a TLS certificate) need a staging
        // directory; 50 MiB caps the upload, anything larger is a 400. A pack
        // upload gets its own, larger bound: a `.dspack` carries a profile's
        // files, and 200 MiB is the documented cap.
        java.nio.file.Path uploadStaging;
        try {
            uploadStaging = config.dataDir.resolve("tmp");
            java.nio.file.Files.createDirectories(uploadStaging);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot create the upload staging directory in " + config.dataDir, e);
        }
        jakarta.servlet.MultipartConfigElement multipart = new jakarta.servlet.MultipartConfigElement(
                uploadStaging.toString(), 50L * 1024 * 1024, 55L * 1024 * 1024, 1024 * 1024);
        jakarta.servlet.MultipartConfigElement packMultipart = new jakarta.servlet.MultipartConfigElement(
                uploadStaging.toString(), 200L * 1024 * 1024, 210L * 1024 * 1024, 1024 * 1024);
        // 壁纸上传有自己的体积上限（见 WallpaperApiServlet），不跟着整合包那 200 MiB 走。
        jakarta.servlet.MultipartConfigElement wallpaperMultipart =
                org.jackhuang.hmcl.web.http.WallpaperApiServlet.multipartConfig(uploadStaging);

        // The TLS settings endpoint switches this very server to TLS at
        // runtime; the reference is filled once the instance exists.
        java.util.concurrent.atomic.AtomicReference<HdslServer> self = new java.util.concurrent.atomic.AtomicReference<>();

        // The developer credential and its elevation gate the web terminal and
        // the other sensitive developer routes (docs/web-shell.md §4.1).
        org.jackhuang.hmcl.web.developer.DeveloperAuth developerAuth;
        org.jackhuang.hmcl.web.developer.DeveloperAuth agentAuth;
        try {
            developerAuth = org.jackhuang.hmcl.web.developer.DeveloperAuth.open(config.dataDir);
            // A second secret, in its own file: the lesser one an operator can
            // hand to an agent without handing over the panel.
            agentAuth = org.jackhuang.hmcl.web.developer.DeveloperAuth.openAgent(config.dataDir);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot open the developer credential store in "
                    + config.dataDir, e);
        }
        java.util.function.BooleanSupplier tlsActive = () -> {
            HdslServer live = self.get();
            return live != null && live.tlsActive();
        };

        // Panel chat (docs/chat.md): SQLite under <data>/chat, one maintenance
        // thread, and the turn manager behind /api/chat/*. It is built after
        // the session gate and elevation store because its servlet needs both.
        //
        // Chat is the only feature backed by a native library (sqlite-jdbc
        // unpacks its platform binary at runtime). When that fails — an
        // unsupported arch, a read-only `java.io.tmpdir`, a missing libc — the
        // rest of the panel must still boot: chat becomes 404 instead of
        // turning a working panel into a dead container.
        ChatModule chat;
        try {
            chat = ChatModule.create(config, eventBus);
        } catch (RuntimeException | LinkageError e) {
            // A native load failure surfaces as UnsatisfiedLinkError (an Error),
            // not as an exception; both must leave the rest of the panel running.
            logger.warning("Chat is disabled: the chat store could not be opened", e);
            chat = null;
        }

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addFilter(authFilter, "/api/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(authFilter, "/i/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(authFilter, "/ws/*", EnumSet.of(DispatcherType.REQUEST));
        // The emergency stop and the developer guard, in that order, after the
        // session gate: an unauthenticated request must still read as 401, not
        // as a guard refusal (docs/web-shell.md §4.1).
        org.jackhuang.hmcl.web.developer.DeveloperGuardFilter guardFilter =
                new org.jackhuang.hmcl.web.developer.DeveloperGuardFilter(
                        config, developerAuth, developerElevation, authService, emergencyStop, tlsActive);
        context.addFilter(new FilterHolder(guardFilter), "/api/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(new FilterHolder(guardFilter), "/i/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new org.jackhuang.hmcl.web.system.SystemApiServlet(
                systemMetrics, config, startedAtMillis::get, mainConnector::getLocalPort)), "/api/system/*");
        context.addServlet(new ServletHolder(new ApiServlet(
                authService, authFilter, config.auth.disabled, Metadata.VERSION, startedAtMillis::get,
                developerElevation)), "/api/*");
        ServletHolder instancesHolder = new ServletHolder(new InstancesApiServlet(runtime, taskService, config, terminals, terminalTickets,
                authService, developerAuth, developerElevation, tlsActive, emergencyStop));
        instancesHolder.getRegistration().setMultipartConfig(multipart);
        context.addServlet(instancesHolder, "/api/instances/*");
        context.addServlet(new ServletHolder(new VersionsApiServlet(taskService)), "/api/versions");
        context.addServlet(new ServletHolder(new TasksApiServlet(taskService)), "/api/tasks/*");
        context.addServlet(new ServletHolder(new NodeApiServlet(taskService, config)), "/api/node/*");
        context.addServlet(new ServletHolder(new PluginsApiServlet()), "/api/plugins/*");
        // The curated terminal-plugin list lives under its own prefix: it is not the
        // community catalogue (that is `/api/plugins/catalog`) and it has no
        // per-instance state of its own to hang off an instance path.
        context.addServlet(new ServletHolder(new DshApiServlet()), "/api/dsh/*");
        ServletHolder packsHolder = new ServletHolder(new PacksApiServlet(runtime, taskService, config));
        packsHolder.getRegistration().setMultipartConfig(packMultipart);
        context.addServlet(packsHolder, "/api/packs/*");
        context.addServlet(new ServletHolder(new ExportsApiServlet(config)), "/api/exports/*");
        context.addServlet(new ServletHolder(new DoctorApiServlet()), "/api/doctor");
        context.addServlet(new ServletHolder(new AccountsApiServlet(eventBus)), "/api/accounts/*");
        // Panel chat: REST commands/history/upload; token deltas ride /ws.
        // No chat module, no servlet: `/api/chat/*` then falls through to the
        // generic 404 instead of answering from a half-initialized store.
        if (chat != null) {
            ServletHolder chatHolder = new ServletHolder(new ChatApiServlet(
                    chat.service(), config, authService, developerElevation, tlsActive));
            chatHolder.getRegistration().setMultipartConfig(new jakarta.servlet.MultipartConfigElement(
                    uploadStaging.toString(), 60L * 1024 * 1024, 65L * 1024 * 1024, 1024 * 1024));
            context.addServlet(chatHolder, "/api/chat/*");
        }
        // The no-JavaScript surface (docs/plain-surface.md). Registered even when
        // chat is unavailable: /plain/old is the landing page for browsers that
        // cannot run the SPA, and it must answer on a degraded install too.
        MarkdownSsr plainSsr = new MarkdownSsr(config.dataDir,
                config.dataDir.resolve("plain-md"), config.plain.ssrWorkers);
        context.addServlet(new ServletHolder(new PlainChatServlet(
                chat == null ? null : chat.service(), authService, authFilter,
                config.auth.disabled, config.basePath, developerAuth, developerElevation,
                tlsActive, config, plainSsr)), "/plain/*");
        // Render the transcript while the turn is still fresh instead of making
        // the first reader wait for one Node process per message.
        if (chat != null) {
            chat.service().turns().setMessageFinalizer((messageId, content, sourcesJson) -> {
                if (config.plain.enhanced) {
                    plainSsr.prefetch(messageId, content, sourcesJson);
                }
            });
        }
        context.addServlet(new ServletHolder(new VendorsApiServlet()), "/api/vendors");
        context.addServlet(new ServletHolder(new ModelsApiServlet()), "/api/models/*");
        context.addServlet(new ServletHolder(new ZcodeApiServlet(config, terminals, terminalTickets,
                authService, developerAuth, developerElevation, tlsActive, emergencyStop)), "/api/zcode/*");
        context.addServlet(new ServletHolder(new BrandApiServlet(
                config, taskService, terminals, terminalTickets,
                authService, developerAuth, developerElevation, tlsActive, emergencyStop)), "/api/brands/*");
        context.addServlet(new ServletHolder(new org.jackhuang.hmcl.web.developer.DeveloperApiServlet(
                developerAuth, agentAuth, developerElevation, authService, config, tlsActive,
                emergencyStop, terminals, runtime, peerIdentity, mainConnector::getLocalPort)),
                "/api/developer/*");
        // Registered before the `/api/settings/*` holder below and matched by its longer prefix:
        // the download source is a launcher setting, and TLS is the other thing this path serves.
        context.addServlet(new ServletHolder(new RegistrySettingsApiServlet()), "/api/settings/registry/*");
        // 出站 TLS 的一次性授权（代理/加速器的根证书不在 Java 的信任库里时）。
        context.addServlet(new ServletHolder(
                new org.jackhuang.hmcl.web.http.NetworkApiServlet(config)), "/api/network/*");
        // 日志保留窗口。比 `/api/settings/*` 的 TLS 更长的前缀，Jetty 按最长匹配。
        context.addServlet(new ServletHolder(new LogRetentionApiServlet(
                config, developerElevation, authService, tlsActive)), "/api/settings/logs/*");
        // 纯文本版（legacy 表面）的增强开关，同一条最长前缀规则。
        context.addServlet(new ServletHolder(new PlainSettingsApiServlet(
                config, developerElevation, authService, tlsActive, plainSsr)), "/api/settings/plain");
        // The brand page's own data (sources + policy). Same reason for the longer prefix: the
        // `/api/settings/*` holder below is TLS, and Jetty matches the longest path spec.
        context.addServlet(new ServletHolder(new BrandSettingsApiServlet(
                config, developerElevation, authService, tlsActive)), "/api/settings/brands/*");
        ServletHolder tlsHolder = new ServletHolder(new TlsApiServlet(config, certificates, self::get));
        tlsHolder.getRegistration().setMultipartConfig(multipart);
        context.addServlet(tlsHolder, "/api/settings/*");
        // 壁纸：外观是用户自己的事，不需要开发者提权；会话门由 /api/* 的 AuthFilter 负责。
        ServletHolder wallpaperHolder = new ServletHolder(
                new org.jackhuang.hmcl.web.http.WallpaperApiServlet(config.dataDir));
        wallpaperHolder.getRegistration().setMultipartConfig(wallpaperMultipart);
        context.addServlet(wallpaperHolder, "/api/wallpapers/*");

        // The transcode surface (docs/transcode.md). Off by default: while the
        // feature is DISABLED the gate answers 404 for everything but the enable
        // flow, so the panel looks exactly like one without this feature. The
        // state lives in <data>/transcode/feature.json, not server.yaml.
        org.jackhuang.hmcl.web.transcode.TranscodeModule transcode =
                new org.jackhuang.hmcl.web.transcode.TranscodeModule(config, eventBus);
        context.addFilter(new FilterHolder(new org.jackhuang.hmcl.web.transcode.TranscodeFeatureFilter(
                transcode.feature(), config.basePath)), "/api/transcode/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new org.jackhuang.hmcl.web.transcode.TranscodeApiServlet(
                transcode, authService, developerAuth, developerElevation, tlsActive)),
                "/api/transcode/*");

        // The Minecraft surface (docs/minecraft.md). Off by default: while the
        // feature is DISABLED the gate answers 404 for everything but the enable
        // flow, so the panel looks exactly like one without this feature.
        org.jackhuang.hmcl.web.minecraft.McFeatureState mcFeature =
                new org.jackhuang.hmcl.web.minecraft.McFeatureState(
                        config.dataDir.resolve("minecraft").resolve("feature.json"));

        // 服务器（存档 + server.jar）放哪：`mc.dir` → `HDSL_MC_DIR` → `$HOME/mc`。
        // 镜像里 HOME=/home/hdsl 且已建好 /home/hdsl/mc，compose 与 docker-install.sh
        // 都在这个路径上挂卷/宿主目录，所以重建容器不会把存档带走。
        //
        // HOME 环境变量优先于 JVM 的 `user.home`：systemd 与容器都会设 HOME，而
        // `user.home` 取的是 passwd 里的家目录——两者不一致时（换用户跑、主机与容器
        // 路径不同），按 HOME 走才符合「$HOME/mc」这句话的字面意思。
        String mcDirSetting = System.getenv("HDSL_MC_DIR");
        if (mcDirSetting == null || mcDirSetting.isBlank()) {
            mcDirSetting = config.mc.dir;
        }
        java.nio.file.Path mcRoot;
        if (mcDirSetting == null || mcDirSetting.isBlank()) {
            String home = System.getenv("HOME");
            if (home == null || home.isBlank()) {
                home = System.getProperty("user.home");
            }
            mcRoot = java.nio.file.Path.of(home, "mc");
        } else {
            mcRoot = java.nio.file.Path.of(mcDirSetting.trim()).toAbsolutePath().normalize();
        }
        try {
            java.nio.file.Files.createDirectories(mcRoot);
        } catch (java.io.IOException e) {
            logger.warning("Could not create the Minecraft directory " + mcRoot
                    + "; creating an instance will fail until it exists and is writable", e);
        }
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            String lower = mcRoot.toString().toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("systemprofile") || lower.startsWith("c:\\windows")) {
                logger.warning("Minecraft servers would live under a system-account path: " + mcRoot
                        + ". Run the panel as a dedicated non-admin user, or set HDSL_MC_DIR / mc.dir"
                        + " to a folder you control.");
            }
            if (mcRoot.toString().length() > 120) {
                logger.warning("The Minecraft server path is " + mcRoot.toString().length()
                        + " characters long; Windows' default MAX_PATH is 260. Prefer a short HDSL_MC_DIR.");
            }
        }
        if (!java.nio.file.Files.isWritable(mcRoot)) {
            logger.warning("The Minecraft server directory is not writable: " + mcRoot);
        }
        // 旧位置（<data>/minecraft/instances）只读兼容：升级前的服务器还在那儿。
        java.nio.file.Path mcLegacyRoot = config.dataDir.resolve("minecraft").resolve("instances");
        logger.info("Minecraft servers live in " + mcRoot
                + (mcLegacyRoot.toFile().isDirectory() ? " (legacy root also read: " + mcLegacyRoot + ")" : ""));
        org.jackhuang.hmcl.web.minecraft.McInstanceManager mcInstances =
                new org.jackhuang.hmcl.web.minecraft.McInstanceManager(mcRoot, mcLegacyRoot);
        String mcJava = config.mc.javaPath == null || config.mc.javaPath.isBlank()
                ? "java" : config.mc.javaPath;
        java.nio.file.Path mcVersionsRoot = config.dataDir.resolve("minecraft").resolve("versions");
        // 面板自托管的 JRE（Adoptium）：`<data>/minecraft/java/<feature>/`。和 Node
        // runtime 同一个思路——版本化、隔离、一次调用删掉；`mc.java_path` 仍然优先。
        java.nio.file.Path mcJavaRoot = config.dataDir.resolve("minecraft").resolve("java");
        org.jackhuang.hmcl.web.minecraft.McJavaRuntimes mcJavaRuntimes =
                new org.jackhuang.hmcl.web.minecraft.McJavaRuntimes(mcJavaRoot);
        org.jackhuang.hmcl.web.minecraft.McRuntime mcRuntime =
                new org.jackhuang.hmcl.web.minecraft.McRuntime(
                        mcInstances, terminals, mcFeature, mcJava, mcVersionsRoot, mcJavaRoot);
        org.jackhuang.hmcl.web.minecraft.McRuntime.JavaInfo mcJavaInfo = mcRuntime.javaInfo();
        logger.info("Minecraft console: " + org.jackhuang.hmcl.web.minecraft.McRuntime.consoleKind()
                + "; java: " + (mcJavaInfo.path().isBlank() ? "(none)" : mcJavaInfo.path())
                + (mcJavaInfo.ok() ? " (" + mcJavaInfo.version() + ")" : " — " + mcJavaInfo.error()));
        org.jackhuang.hmcl.web.minecraft.McVersions mcVersions =
                new org.jackhuang.hmcl.web.minecraft.McVersions(mcVersionsRoot);
        // 下载源设置：哪一个镜像、什么顺序、还剩多少配额。落盘在
        // `<data>/minecraft/sources.json`，和实例目录分开——换源不该影响存档。
        org.jackhuang.hmcl.web.minecraft.McSources mcSources =
                new org.jackhuang.hmcl.web.minecraft.McSources(
                        config.dataDir.resolve("minecraft").resolve("sources.json"));
        org.jackhuang.hmcl.web.minecraft.McCores mcCores =
                new org.jackhuang.hmcl.web.minecraft.McCores(mcVersions,
                        config.dataDir.resolve("minecraft").resolve("cores"), mcSources);
        org.jackhuang.hmcl.web.minecraft.McBackups mcBackups =
                new org.jackhuang.hmcl.web.minecraft.McBackups(
                        config.dataDir.resolve("minecraft").resolve("backups"));
        context.addFilter(new FilterHolder(new org.jackhuang.hmcl.web.minecraft.McFeatureFilter(
                mcFeature, config.basePath)), "/api/mc/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new org.jackhuang.hmcl.web.minecraft.McApiServlet(
                config, mcFeature, mcInstances, mcRuntime, mcVersions, mcCores, mcJavaRuntimes,
                mcBackups, taskService,
                terminalTickets, authService, developerAuth, developerElevation, tlsActive, emergencyStop)), "/api/mc/*");

        context.addServlet(new ServletHolder(new InstanceProxyServlet(taskService, config.basePath)), "/i/*");
        // A panel killed mid source-build leaves a half-built release next to a
        // complete `<version>.previous`; put it back before anything lists
        // releases (otherwise that version reads as installed but cannot start).
        // Deliberately *after* the proxy servlet is constructed: touching the
        // installer initialises its own HttpClient, and the JDK freezes the
        // restricted-header allow-list at that moment (see HostHeaderSupport).
        org.jackhuang.hmcl.web.brand.BrandInstaller.reconcileOctopReleases(
                config.dataDir.resolve("brands").resolve(org.jackhuang.hmcl.web.brand.Brand.OCTOP.id()));
        context.addServlet(new ServletHolder(new StaticServlet(config.basePath)), "/");
        // Anything a servlet lets escape becomes a JSON 500, not Jetty's HTML
        // error page — the frontend reads these bodies as an API contract.
        context.addFilter(new FilterHolder(new ApiGuardFilter()), "/api/*", EnumSet.of(DispatcherType.REQUEST));

        // `/ws` rides the jakarta WebSocket container of the context, which
        // funnels upgrades through the filter chain — that is what makes the
        // AuthFilter above the 401 gate for unauthenticated handshakes.
        JakartaWebSocketServletContainerInitializer.configure(context,
                (servletContext, container) -> {
                    container.addEndpoint(WsGateway.class);
                    container.addEndpoint(org.jackhuang.hmcl.web.terminal.TerminalWebSocket.class);
                });

        // Upgrades under `/i/*` are relayed to the instance's own WebSocket
        // endpoint. Jakarta endpoint paths are single-segment URI templates
        // and cannot express the multi-segment wildcard `/i/<id>/<rest…>`, so
        // the relay uses the core mapping API, which can.
        WebSocketUpgradeHandler instanceUpgrades = WebSocketUpgradeHandler.from(server, container -> {
            // Jetty's default frame and message limit is 64 KiB; a dsh payload
            // (a session history, a tool result) is routinely larger, and the
            // relay would fail the whole stream with 1009. Raised here as well
            // as on the client side.
            container.setMaxFrameSize(InstanceProxyWebSocket.MAX_MESSAGE_BYTES);
            container.setMaxTextMessageSize(InstanceProxyWebSocket.MAX_MESSAGE_BYTES);
            container.setMaxBinaryMessageSize(InstanceProxyWebSocket.MAX_MESSAGE_BYTES);
            container.addMapping("/i/*",
                    (upgradeRequest, upgradeResponse, callback) -> createRelay(
                            authService, config.auth.disabled, proxyWebSocketClient,
                            config, developerElevation, emergencyStop,
                            upgradeRequest, upgradeResponse, callback));
        });

        // The jakarta container's own upgrade filter prepends itself ahead of
        // every servlet filter, so the AuthFilter never sees a `/ws` upgrade
        // and an unauthenticated handshake would surface as a 500 from the
        // endpoint configurator. This handler is the gate instead: upgrade
        // requests to `/ws` without a session are answered 401 right here,
        // before the context ever negotiates.
        WsUpgradeAuthGate wsAuthGate = new WsUpgradeAuthGate(authService, config.auth.disabled);

        Handler handler = new Handler.Sequence(instanceUpgrades, wsAuthGate, context);
        if (config.https.enabled) {
            // Installed whenever TLS is on: any request that did not arrive over
            // TLS — on the main connector's cleartext path or on the plain-HTTP
            // redirect connector — is answered with the 308 to HTTPS.
            handler = new Handler.Sequence(
                    new HttpsRedirectHandler(mainConnector),
                    instanceUpgrades,
                    wsAuthGate,
                    context);
        }
        server.setHandler(handler);

        // Fail fast if something ordered badly: a broken Host passthrough is a
        // 500 on every /i/* mount, and finding that out at boot beats finding it
        // out from users.
        org.jackhuang.hmcl.web.http.HostHeaderSupport.verify();

        HdslServer built = new HdslServer(server, httpConfig, mainConnector, redirectConnector, ssl,
                proxyWebSocketClient, acpSessions, terminals, mcRuntime, systemMetrics, chat,
                logger, startedAtMillis);
        self.set(built);
        return built;
    }

    /// The 401 gate in front of the `/ws` WebSocket endpoint: answers
    /// unauthenticated handshakes directly, passes everything else through.
    private static final class WsUpgradeAuthGate extends Handler.Abstract {
        private final AuthService authService;
        private final boolean authDisabled;

        private WsUpgradeAuthGate(AuthService authService, boolean authDisabled) {
            this.authService = authService;
            this.authDisabled = authDisabled;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            if (authDisabled || !request.getHeaders().contains(HttpHeader.UPGRADE)) {
                return false;
            }
            String path = request.getHttpURI().getPath();
            if (!path.equals("/ws") && !path.startsWith("/ws/")) {
                return false;
            }
            if (hasSession(authService, request.getHeaders())) {
                return false;
            }
            response.setStatus(401);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=utf-8");
            byte[] body = "{\"error\":\"unauthorized\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            response.write(true, java.nio.ByteBuffer.wrap(body), callback);
            return true;
        }
    }

    /// The upgrade creator behind `/i/*`: an unauthenticated handshake is
    /// answered 401 in place; anything else becomes a relay whose upstream
    /// connection is opened when the endpoint comes up.
    private static @Nullable InstanceProxyWebSocket createRelay(
            AuthService authService, boolean authDisabled, WebSocketClient proxyWebSocketClient,
            ServerConfig config,
            org.jackhuang.hmcl.web.developer.DeveloperElevation elevation,
            org.jackhuang.hmcl.web.emergency.EmergencyStop emergency,
            org.eclipse.jetty.websocket.server.ServerUpgradeRequest upgradeRequest,
            org.eclipse.jetty.websocket.server.ServerUpgradeResponse upgradeResponse, Callback callback) {
        if (!authDisabled && !hasSession(authService, upgradeRequest.getHeaders())) {
            upgradeResponse.setStatus(401);
            callback.succeeded();
            return null;
        }
        // The stop and the guard have to be judged here as well: a WebSocket
        // upgrade never reaches the servlet filters, so a gate that only lives
        // in a Filter would leave the stream open (docs/web-shell.md §4.1).
        if (emergency.isEngaged()) {
            upgradeResponse.setStatus(503);
            callback.succeeded();
            return null;
        }
        if (config.developerGuard.scope.coversProxy() && !elevated(authService, elevation, upgradeRequest)) {
            upgradeResponse.setStatus(403);
            callback.succeeded();
            return null;
        }
        String path = upgradeRequest.getHttpURI().getPath();
        if (!config.basePath.isEmpty() && path.startsWith(config.basePath)) {
            // The upgrade handler sees the full URI, base path included.
            path = path.substring(config.basePath.length());
        }
        String rest = path.startsWith("/i/") ? path.substring("/i/".length()) : "";
        int slash = rest.indexOf('/');
        String instanceId = slash < 0 ? rest : rest.substring(0, slash);
        String targetPath = slash < 0 ? "/" : rest.substring(slash);
        if (upgradeRequest.getHttpURI().getQuery() != null) {
            targetPath += "?" + upgradeRequest.getHttpURI().getQuery();
        }
        if (instanceId.isEmpty()) {
            upgradeResponse.setStatus(404);
            callback.succeeded();
            return null;
        }
        List<HttpField> headers = new ArrayList<>();
        upgradeRequest.getHeaders().forEach(headers::add);
        return new InstanceProxyWebSocket(proxyWebSocketClient, instanceId, targetPath, headers,
                config.basePath);
    }

    /// Whether the upgrade request carries a valid `hdsl_session` cookie.
    private static boolean hasSession(AuthService authService, org.eclipse.jetty.http.HttpFields headers) {
        String cookie = headers.get(HttpHeader.COOKIE);
        if (cookie == null) {
            return false;
        }
        for (String pair : cookie.split(";")) {
            String trimmed = pair.trim();
            if (trimmed.startsWith(AuthFilter.SESSION_COOKIE + "=")) {
                String token = trimmed.substring(AuthFilter.SESSION_COOKIE.length() + 1);
                return authService.verify(token).isPresent();
            }
        }
        return false;
    }

    /// Whether the upgrade request proves a developer elevation — either the
    /// cookie session's, or a bearer token minted by `/api/developer/elevate`.
    private static boolean elevated(AuthService authService,
                                     org.jackhuang.hmcl.web.developer.DeveloperElevation elevation,
                                     org.eclipse.jetty.websocket.server.ServerUpgradeRequest request) {
        org.eclipse.jetty.http.HttpFields headers = request.getHeaders();
        String cookie = headers.get(HttpHeader.COOKIE);
        if (cookie != null) {
            for (String pair : cookie.split(";")) {
                String trimmed = pair.trim();
                if (trimmed.startsWith(AuthFilter.SESSION_COOKIE + "=")) {
                    String token = trimmed.substring(AuthFilter.SESSION_COOKIE.length() + 1);
                    if (!authService.verify(token).isPresent()) {
                        return false;
                    }
                    return elevation.require(
                            org.jackhuang.hmcl.web.developer.DeveloperElevation.key(token, null), true);
                }
            }
        }
        String authorization = headers.get(HttpHeader.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return elevation.requireToken(authorization.substring(7).trim());
        }
        return false;
    }

    /// Binds the connectors and registers the shutdown hook.
    public void start() throws Exception {
        proxyWebSocketClient.start();
        server.start();
        startedAtMillis.set(System.currentTimeMillis());
        // Do not touch the registries here: startup must print its banner and
        // answer health checks without waiting for GitHub. The idle loop reads
        // the disk cache immediately and only goes to the network after the
        // panel has been up for a few minutes and stayed quiet.
        BrandCatalog.startBackgroundRefresh();
        if (hookInstalled.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception e) {
                    logger.warning("Failed to stop the server during shutdown", e);
                }
            }, "HDSL-web shutdown"));
        }
    }

    /// The port the main connector actually bound. With `port: 0` this is the
    /// ephemeral port the OS picked; meaningful after [start].
    public int getPort() {
        return mainConnector.getLocalPort();
    }

    /// The port of the HTTP→HTTPS redirect connector, or -1 without one.
    public int getRedirectPort() {
        return redirectConnector == null ? -1 : redirectConnector.getLocalPort();
    }

    /// The live [SslContextFactory] behind the main connector, for certificate
    /// hot-reload via `SslContextFactory.reload(...)`; empty when HTTP only.
    public Optional<SslContextFactory.Server> getSslContextFactory() {
        return Optional.ofNullable(sslContextFactory);
    }

    /// The registry of running terminals — one per instance, and the panel is
    /// their parent process. [stop] closes it; the instance routes close the
    /// entries they own before they delete or take over an instance. Exposed
    /// so the lifecycle tests can watch an instance's terminal the way the
    /// route leaves it, without a PTY in the picture.
    public org.jackhuang.hmcl.web.terminal.TerminalManager terminals() {
        return terminals;
    }

    /// Whether the main connector currently speaks TLS.
    public boolean isTls() {
        return sslContextFactory != null;
    }

    /// Whether the panel is serving HTTPS **right now** — the guard the
    /// developer endpoints check instead of trusting headers.
    /// Reads the live connector (an [SslConnectionFactory] on it) so a
    /// runtime switch is reflected the moment it completes; the boot-time
    /// identity reference is the fallback.
    public boolean tlsActive() {
        if (sslContextFactory != null) {
            return true;
        }
        ServerConnector connector = mainConnector;
        return connector != null && connector.getConnectionFactory(SslConnectionFactory.class) != null;
    }

    /// The outcome of a TLS switch attempt: on success `restartRequired` is
    /// false; on failure the plain-HTTP connector was put back and a restart
    /// would apply the uploaded identity instead.
    public record TlsSwitch(boolean ok, boolean restartRequired, @Nullable String error) {
    }

    /// Swaps the live identity on a TLS connector without dropping the port.
    ///
    /// @param certificates the manager holding the new identity
    /// @return what happened
    public synchronized TlsSwitch reloadTls(CertificateManager certificates) {
        SslContextFactory.Server ssl = sslContextFactory;
        if (ssl == null) {
            return new TlsSwitch(false, true, "the server is not speaking TLS");
        }
        try {
            ssl.reload(certificates::applyTo);
            logger.info("TLS identity hot-reloaded from the uploaded certificate");
            return new TlsSwitch(true, false, null);
        } catch (Exception e) {
            logger.warning("Failed to reload the TLS identity", e);
            return new TlsSwitch(false, true, String.valueOf(e.getMessage()));
        }
    }

    /// The customizer both TLS connectors install.
    ///
    /// Its SNI host check is deliberately off. The panel serves exactly one
    /// identity, so the check has nothing to protect — but it does turn a
    /// probe that reaches the port by address (`curl https://127.0.0.1:…`,
    /// which sends no SNI and therefore matches no certificate in the
    /// keystore) into a 400 `Invalid SNI`, which is how the container's own
    /// health check kept failing while the server was perfectly healthy.
    /// `sniRequired` stays at its default `false`.
    private static SecureRequestCustomizer secureRequestCustomizer() {
        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniHostCheck(false);
        return customizer;
    }

    /// Switches a plain-HTTP server to TLS at runtime: the plain connector is
    /// gracefully shut down — its accept socket closes immediately (an
    /// interrupted `accept()` closes the channel), freeing the port, while the
    /// connections already on it drain — and a TLS connector takes its place on
    /// the same host and port.
    ///
    /// The graceful shutdown is what makes this callable from a request being
    /// served by the very connector being replaced (the certificate upload is):
    /// a hard `stop()` would cut that connection before its answer is written.
    /// The retired connector is stopped for real once its last connection ends.
    ///
    /// Jetty supports runtime connector swaps, so this is a connector exchange,
    /// not a restart. On any failure a fresh plain connector is put back and
    /// the answer says a restart would apply the certificate instead — the
    /// upload has already been written to `server.yaml`, so a restart does
    /// exactly that.
    ///
    /// @param certificates the manager holding the uploaded identity
    /// @return what happened
    public synchronized TlsSwitch enableTls(CertificateManager certificates) {
        if (sslContextFactory != null) {
            // Already TLS; the caller should have reloaded instead.
            return reloadTls(certificates);
        }
        ServerConnector previous = mainConnector;
        String host = previous.getHost();
        int port = previous.getLocalPort();

        SslContextFactory.Server ssl = new SslContextFactory.Server();
        certificates.applyTo(ssl);
        HttpConfiguration httpsConfig = new HttpConfiguration(baseHttpConfig);
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(port);
        httpsConfig.addCustomizer(secureRequestCustomizer());
        ServerConnector tlsConnector = new ServerConnector(server, ssl, new HttpConnectionFactory(httpsConfig));
        tlsConnector.setHost(host);
        tlsConnector.setPort(port);

        java.util.concurrent.CompletableFuture<Void> drained;
        try {
            // Graceful shutdown only — do NOT removeConnector here: removing a
            // started connector stops it (Jetty's bean lifecycle), which would
            // cut the very connection the upload request rides on. The retired
            // connector is removed and stopped once drained.
            drained = previous.shutdown();
        } catch (Exception e) {
            logger.warning("TLS switch failed: the HTTP connector would not stop accepting", e);
            return new TlsSwitch(false, true, String.valueOf(e.getMessage()));
        }

        try {
            server.addConnector(tlsConnector);
            startAwaitingPort(tlsConnector);
        } catch (Exception e) {
            logger.warning("TLS switch failed; restoring the plain-HTTP connector", e);
            try {
                tlsConnector.stop();
            } catch (Exception ignored) {
                // Never started or already gone.
            }
            try {
                server.removeConnector(tlsConnector);
            } catch (Exception ignored) {
                // Not registered.
            }
            ServerConnector plain = new ServerConnector(server, new HttpConnectionFactory(baseHttpConfig));
            plain.setHost(host);
            plain.setPort(port);
            try {
                server.addConnector(plain);
                startAwaitingPort(plain);
                mainConnector = plain;
            } catch (Exception rollback) {
                logger.error("Could not restore the plain-HTTP connector on port " + port, rollback);
                retireWhenDrained(previous, drained);
                return new TlsSwitch(false, true, String.valueOf(e.getMessage())
                        + " (and restoring HTTP failed: " + rollback.getMessage() + ")");
            }
            retireWhenDrained(previous, drained);
            return new TlsSwitch(false, true, String.valueOf(e.getMessage()));
        }

        this.sslContextFactory = ssl;
        this.mainConnector = tlsConnector;
        retireWhenDrained(previous, drained);
        logger.info("TLS enabled at runtime: port " + port + " now speaks HTTPS");
        return new TlsSwitch(true, false, null);
    }

    /// Starts a connector on a port the retired connector is in the middle of
    /// releasing: the accept thread needs a moment after the interrupt for the
    /// channel to close, so a handful of retries bridges the gap before the
    /// switch is declared failed.
    private static void startAwaitingPort(ServerConnector connector) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            try {
                connector.start();
                return;
            } catch (Exception e) {
                last = e;
                if (!(e instanceof java.io.IOException) && !(e.getCause() instanceof java.io.IOException)) {
                    throw e;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last == null ? new java.io.IOException("the port never came free") : last;
    }

    /// Removes and stops a gracefully-retired connector once its remaining
    /// connections have ended (with a bound, so a stuck connection cannot keep
    /// it forever). Removal is what stops it — Jetty stops a started bean on
    /// removal — so it must not happen before the drain.
    private void retireWhenDrained(ServerConnector retired,
                                   java.util.concurrent.CompletableFuture<Void> drained) {
        Thread closer = new Thread(() -> {
            try {
                drained.get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // The bound elapsed; stop it anyway.
            }
            try {
                server.removeConnector(retired);
            } catch (Exception e) {
                logger.info("Could not remove the retired connector: " + e.getMessage());
            }
            try {
                retired.stop();
            } catch (Exception e) {
                logger.info("Could not stop the retired connector: " + e.getMessage());
            }
        }, "HDSL-web connector-retire");
        closer.setDaemon(true);
        closer.start();
    }

    /// Stops Jetty, waiting for in-flight connections to drain, and ends every
    /// ACP console the bridge is holding.
    public void stop() throws Exception {
        // Brand instances are children of the panel too — and unlike the
        // terminal sessions they are long-running services (`kimi web`, a
        // Hermes gateway). `docker stop` signals PID 1 only, so without this a
        // Hermes gateway outlives the panel until the container's grace period
        // expires, and on a bare deployment for good. The deadline is shared:
        // N instances must not each spend their own 40 s window.
        org.jackhuang.hmcl.web.brand.BrandRuntime.stopAll(25_000L);
        // Minecraft servers are children too. On POSIX they are terminal
        // sessions, which `terminals.close()` would take down; on Windows they
        // are plain pipe processes that would otherwise outlive the panel and
        // keep the game port. Ask every server to `stop`, then kill stragglers.
        mcRuntime.stopAll(Duration.ofSeconds(15));
        // The terminal processes are children of the panel; closing the
        // manager takes their process groups with it.
        terminals.close();
        acpSessions.closeAll();
        if (chat != null) {
            chat.close();
        }
        systemMetrics.close();
        server.stop();
        proxyWebSocketClient.stop();
    }

    /// Blocks the calling thread until the server has stopped.
    public void join() throws InterruptedException {
        server.join();
    }

    /// Answers **every request that did not arrive over TLS** with
    /// `308 Location: https://<host>:<mainPort><path>` — both the cleartext path
    /// of the main connector (`https.redirect_same_port`, so a bare `http://<ip>`
    /// entry lands on the panel instead of `ERR_INVALID_HTTP_RESPONSE`) and the
    /// plain-HTTP connector (`https.redirect_http`). Installed only when TLS is
    /// on; a request that *did* arrive over TLS passes through untouched, so
    /// panel content never travels in cleartext.
    private static final class HttpsRedirectHandler extends Handler.Abstract {
        private final ServerConnector mainConnector;

        private HttpsRedirectHandler(ServerConnector mainConnector) {
            this.mainConnector = mainConnector;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            if (request.isSecure()) {
                return false;
            }
            String host = request.getHeaders().get(HttpHeader.HOST);
            // glm-5.2: Validate the Host header before using it to construct the
            // redirect Location. A client controls the Host value it sends, so
            // copying it raw can point the redirect at a domain the attacker
            // controls. Reject values that contain path separators or protocol
            // prefixes and fall back to the actual server name from the
            // connection metadata instead.
            String bareHost;
            if (host == null || host.isBlank()) {
                bareHost = "localhost";
            } else if (host.contains("/") || host.contains("\\")
                    || host.contains("http://") || host.contains("https://")) {
                String serverName = Request.getServerName(request);
                bareHost = serverName != null && !serverName.isBlank()
                        ? stripPort(serverName) : "localhost";
            } else {
                bareHost = stripPort(host);
            }
            int mainPort = mainConnector.getLocalPort();

            HttpURI uri = request.getHttpURI();
            StringBuilder location = new StringBuilder("https://").append(bareHost);
            if (mainPort != 443) {
                location.append(':').append(mainPort);
            }
            location.append(uri.getPath());
            if (uri.getQuery() != null) {
                location.append('?').append(uri.getQuery());
            }

            response.setStatus(308);
            response.getHeaders().put(HttpHeader.LOCATION, location.toString());
            callback.succeeded();
            return true;
        }

        /// Drops the `:port` suffix of a Host header — shared with the servlets.
        private static String stripPort(String host) {
            return org.jackhuang.hmcl.web.http.HostHeaders.stripPort(host);
        }
    }
}
