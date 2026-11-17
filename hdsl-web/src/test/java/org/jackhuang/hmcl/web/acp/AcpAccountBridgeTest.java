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
package org.jackhuang.hmcl.web.acp;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshAccountRoute;
import org.jackhuang.hmcl.dsh.DshAcpClient;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshInstanceSettings;
import org.jackhuang.hmcl.dsh.DshPluginPatch;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Handing an instance's account to its ACP console: the route lands in the console profile's own
/// patch layer, the key travels in the child's environment and nowhere else, a route left by an
/// account this instance no longer names is taken back — and the route goes away again when the
/// console ends.
///
/// The handover is read from the bridge directly ([AcpAccountBridge#prepare]), so no harness and no
/// model vendor are involved: an account's supplier is asked for its models over the network, and
/// these accounts point at a closed port so the answer is the empty list and the account's own model
/// name is what the route is written with.
class AcpAccountBridgeTest {

    @TempDir
    Path tempDir;

    private final List<String> instances = new ArrayList<>();
    private final List<AcpSessionManager> managers = new ArrayList<>();
    private final List<DshAccount> savedAccounts = new ArrayList<>();

    @BeforeEach
    void takeTheAccountsOver() {
        savedAccounts.addAll(SettingsManager.settings().getAccounts());
        SettingsManager.settings().getAccounts().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        for (AcpSessionManager manager : managers) {
            manager.closeAll();
        }
        managers.clear();
        SettingsManager.settings().getAccounts().clear();
        SettingsManager.settings().getAccounts().addAll(savedAccounts);
        savedAccounts.clear();
        for (String id : instances) {
            if (DshInstanceManager.exists(id)) {
                DshInstanceManager.delete(id);
            }
        }
        instances.clear();
    }

    @Test
    void theConsoleProfileIsGivenTheRouteAndTheKeyStaysInTheEnvironment() throws Exception {
        DshInstance instance = instance("acp-account-route");
        DshAccount account = register(deepSeekAccount("1", "sk-console-test"));
        DshInstanceSettings.setAccountKey(instance, account.key());

        AcpAccountBridge bridge = new AcpAccountBridge();
        AcpAccountBridge.Handover handover = bridge.prepare(instance);

        assertEquals("1", handover.route(), "the route is named after the account");
        assertEquals("sk-console-test",
                handover.environment().get(DshAccountRoute.environmentVariable("1")));
        assertEquals("sk-console-test",
                handover.environment().get(DshAccountRoute.WEB_SEARCH_ENVIRONMENT_VARIABLE),
                "a DeepSeek route hands its key to the harness's own web search as well");

        String patch = read(patchFile(instance));
        assertTrue(patch.contains("apiKeyEnv: " + DshAccountRoute.environmentVariable("1")), patch);
        assertTrue(patch.contains("http://127.0.0.1:1/v1"),
                "the route carries the account's own address: " + patch);
        assertTrue(patch.contains("deepseek-flash"), patch);
        assertFalse(patch.contains("sk-console-test"), "the key itself is never written anywhere");

        bridge.release(instance);
        assertFalse(read(patchFile(instance)).contains("apiKeyEnv"),
                "the route goes back out when the console is over");
    }

    @Test
    void aRouteLeftByAnotherAccountIsTakenBack() throws Exception {
        DshInstance instance = instance("acp-account-superseded");
        DshAccount previous = register(thirdPartyAccount("other-brand", "sk-previous"));
        writeRoute(instance, previous);

        DshAccount account = register(deepSeekAccount("1", "sk-console-test"));
        DshInstanceSettings.setAccountKey(instance, account.key());
        new AcpAccountBridge().prepare(instance);

        String patch = read(patchFile(instance));
        assertFalse(patch.contains(DshAccountRoute.environmentVariable("other-brand")), patch);
        assertTrue(patch.contains(DshAccountRoute.environmentVariable("1")), patch);
    }

    @Test
    void anInstanceThatNamesNoAccountIsLeftAlone() throws Exception {
        DshInstance instance = instance("acp-account-none");

        AcpAccountBridge.Handover handover = new AcpAccountBridge().prepare(instance);

        assertNull(handover.route(), "nothing is handed over without an account");
        assertTrue(handover.environment().isEmpty());
        assertFalse(Files.exists(patchFile(instance)), "and no profile patch is written");
    }

    @Test
    void aForeignVendorsAccountIsAlsoMadeTheDefault() throws Exception {
        DshInstance instance = instance("acp-account-third-party");
        DshAccount account = register(thirdPartyAccount("other-brand", "sk-foreign"));
        DshInstanceSettings.setAccountKey(instance, account.key());

        AcpAccountBridge.Handover handover = new AcpAccountBridge().prepare(instance);

        assertEquals("other-brand", handover.route());
        assertTrue(handover.defaultModel(), "a foreign route has to be named or the harness uses its own");
        String settings = read(instance.homeDirectory().resolve("settings.yaml"));
        assertTrue(settings.contains("other-brand"), settings);
        assertFalse(handover.environment().containsKey(DshAccountRoute.WEB_SEARCH_ENVIRONMENT_VARIABLE),
                "the harness's web search is DeepSeek's service and another vendor's key is refused by it");
    }

    @Test
    void stoppingAConsoleGivesWhatTheConnectorWroteBack() throws Exception {
        String id = "acp-account-release";
        DshInstance instance = instance(id);
        Path cancelLog = Files.createDirectories(tempDir.resolve("release-artifacts")).resolve("cancel.log");
        Map<String, String> environment = Map.of(
                AcpStubPeer.CANCEL_LOG_ENV, cancelLog.toString(),
                AcpStubPeer.PROMPT_DELAY_ENV, "0");

        List<String> released = new CopyOnWriteArrayList<>();
        AcpSessionManager.Connector recording = new AcpSessionManager.Connector() {
            @Override
            public DshAcpClient connect(DshInstance subject, Path cwd, DshAcpClient.Listener listener)
                    throws DshException {
                return DshAcpClient.connect(subject, stubCommand(), cwd, environment, listener);
            }

            @Override
            public void release(DshInstance subject) {
                released.add(subject.id());
            }
        };

        EventBus bus = new EventBus();
        List<JsonObject> events = new CopyOnWriteArrayList<>();
        bus.subscribe("instance:" + id, (topic, payload) -> events.add(payload));

        AcpSessionManager manager = new AcpSessionManager(bus, recording);
        managers.add(manager);

        manager.start(id, null);
        assertNotNull(await(events, "acp-ready", 30_000), "the console never came up");

        manager.stop(id);
        assertTrue(awaitRelease(released, id, 15_000),
                "stopping a console must give back what the connector wrote for it");
    }

    /// Puts an account where the launcher keeps them, which is where the instance's own answer
    /// (`accountKey`) is looked up in — an account that is not there is an account an instance
    /// cannot name.
    private static DshAccount register(DshAccount account) {
        SettingsManager.settings().getAccounts().add(account);
        return account;
    }

    /// A DeepSeek account of the launcher's own vendor, pointing at a closed port so the model
    /// listing answers nothing and the account's own model name is what the route is written with.
    private static DshAccount deepSeekAccount(String label, String key) {
        return new DshAccount(DshAccount.AccountKind.OFFICIAL, "deepseek", key,
                "http://127.0.0.1:1/v1", label, "deepseek-flash", null);
    }

    private static DshAccount thirdPartyAccount(String label, String key) {
        return new DshAccount(DshAccount.AccountKind.THIRD_PARTY, "opencode", key,
                "http://127.0.0.1:1/v1", label, "some-model", null);
    }

    private DshInstance instance(String id) throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve(id + "-workspace"));
        if (DshInstanceManager.exists(id)) {
            DshInstanceManager.delete(id);
        }
        DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                workspace, DshHomeMode.ISOLATED, null, List.of(), Map.of());
        instances.add(id);
        Path entry = instance.dshEntryPoint();
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// the bridge tests never execute this file\n");
        return instance;
    }

    /// Writes a route into the console profile the way a console of that account would have.
    private static void writeRoute(DshInstance instance, DshAccount account) throws Exception {
        DshAccountRoute.Prepared prepared = DshAccountRoute.prepare(account).orElseThrow();
        prepared.resolveModels(account);
        DshAccountRoute.apply(instance.homeDirectory(), AcpAccountBridge.PROFILE, prepared);
    }

    private static Path patchFile(DshInstance instance) throws DshException {
        return DshPluginPatch.patchFile(instance.homeDirectory(), AcpAccountBridge.PROFILE);
    }

    private static String read(Path file) throws Exception {
        return Files.isRegularFile(file) ? Files.readString(file) : "";
    }

    private static List<String> stubCommand() {
        return List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                AcpStubPeer.class.getName());
    }

    private static JsonObject await(List<JsonObject> events, String type, long millis)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < deadline) {
            for (JsonObject event : events) {
                if (type.equals(event.get("type").getAsString())) {
                    return event;
                }
            }
            Thread.sleep(25);
        }
        return null;
    }

    private static boolean awaitRelease(List<String> released, String id, long millis)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (System.nanoTime() < deadline) {
            if (released.contains(id)) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }
}
