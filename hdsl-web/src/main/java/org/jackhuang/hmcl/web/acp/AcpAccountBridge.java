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

import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshAccountRoute;
import org.jackhuang.hmcl.dsh.DshAcpClient;
import org.jackhuang.hmcl.dsh.DshDefaultModel;
import org.jackhuang.hmcl.dsh.DshEnvironment;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshLauncher;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.instance.InstanceRuntime;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Hands an instance's account to its ACP console.
///
/// An account reaches a launch, and only a launch: [DshLauncher] writes the supplier route into the
/// patch layer of the profile **it** boots and puts the key in **that** child's environment. A
/// console boots a different profile (`acp`) as a different child, so it used to come up with no
/// supplier and no key at all — every turn ended in the harness's own sentence, `no API key for
/// provider route "deepseek-official"`, naming a route the person never chose. This is the same
/// handover for the console's profile: the account's route goes into `profiles/acp`'s own patch
/// layer, the key travels in the child's environment, and the route is taken back out when the
/// console ends.
///
/// Copied from a launch, deliberately:
///
/// - the route is written into the profile's **own** patch layer, not a `--patch` overlay, for the
///   reasons [DshAccountRoute] gives;
/// - the key travels in a variable named after the route, plus the vendor's own variable for a
///   DeepSeek route (the harness's web search reads that one);
/// - a default model is set only for an account that is not the launcher's own vendor, and only
///   where a launch sets it.
///
/// **Not** copied: the note a launch writes beside the harness's own settings
/// (`settings.yaml` / `.hdsl-injected.json`). That note is one per home and a running web instance
/// is using it — a console that stashed and restored it would be fighting the launch it sits next
/// to. A console therefore touches the profile's patch layer, the child's environment, and (for a
/// foreign vendor's account) the default model; nothing else.
@NotNullByDefault
public final class AcpAccountBridge implements AcpSessionManager.Connector {

    /// The profile a console boots. The same name [DshAcpClient] passes on its own command line.
    static final String PROFILE = "acp";

    /// Instance id → the route this bridge left in that instance's console profile, so [#release]
    /// can take back exactly what was written even if the account has changed since.
    private final ConcurrentHashMap<String, String> written = new ConcurrentHashMap<>();

    /// What a console start handed the harness.
    ///
    /// @param environment the additions for the child's environment, empty when there is no account
    /// @param route       the route written into the console's profile, or `null` when none was
    /// @param defaultModel whether the home's own default model had to be written
    record Handover(Map<String, String> environment, @Nullable String route, boolean defaultModel) {

        /// A handover that handed nothing over.
        static final Handover NONE = new Handover(Map.of(), null, false);
    }

    @Override
    public DshAcpClient connect(DshInstance instance, Path workingDirectory, DshAcpClient.Listener listener)
            throws DshException {
        Handover handover = prepare(instance);
        if (handover.route() == null) {
            // Nothing was handed over, so the console takes its usual path — the harness's own
            // command and environment, exactly as before this bridge existed.
            return DshAcpClient.connect(instance, workingDirectory, listener);
        }

        DshNodeRuntime runtime = DshLauncher.resolveRuntime(instance);
        Path script = instance.dshEntryPoint();
        if (!Files.isRegularFile(script)) {
            throw new DshException("Instance " + instance.id()
                    + " has no DeepSeek Harness of its own; " + script + " is missing");
        }

        // The environment has to be stated in full here: the explicit-command overload of
        // DshAcpClient adds nothing of its own, and the harness reads DSH_HOME to find the home
        // whose sessions and profile this console is meant to work in.
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DSH_HOME", instance.homeDirectory().toString());
        environment.putAll(handover.environment());
        environment.putAll(runtime.pathEnvironment());
        environment.putAll(DshEnvironment.of(instance));

        List<String> command = List.of(runtime.node().toString(), script.toString(), "--profile", PROFILE);
        try {
            return DshAcpClient.connect(instance, command, workingDirectory, environment, listener);
        } catch (DshException | RuntimeException e) {
            // The console never came up, so the route it would have used goes back: a route whose
            // key variable nothing sets is exactly what the harness reports as a missing key.
            release(instance);
            throw e;
        }
    }

    /// Gives back what [#prepare] wrote for an instance's console.
    ///
    /// Idempotent, and a no-op for an instance this bridge never prepared — including one whose
    /// start failed before anything was written.
    @Override
    public void release(DshInstance instance) {
        String route = written.remove(instance.id());
        if (route == null) {
            return;
        }
        try {
            DshAccountRoute.remove(instance.homeDirectory(), PROFILE, route);
        } catch (DshException e) {
            LOG.warning("[acp] could not take the account route " + route + " back out of the console"
                    + " profile of " + instance.id(), e);
        }
    }

    /// Resolves the instance's account and says what the console needs for it.
    ///
    /// Package-visible so a test can read the handover — which environment, which route, which file
    /// — without a harness in the picture. Every failure here is a warning and an empty handover
    /// rather than an exception: a console without a supplier still answers, and it is the person's
    /// own account plumbing that is at fault, not the console.
    ///
    /// @param instance the instance
    /// @return what to hand over, never `null`
    Handover prepare(DshInstance instance) {
        DshAccount account = InstanceRuntime.accountOf(instance);
        if (account == null || !account.carriesAKey()) {
            // No account named, or one with nothing to hand over: unchanged behaviour, and the
            // console is left exactly as it was before this bridge existed.
            return Handover.NONE;
        }

        String route = account.displayName();
        try {
            Path home = instance.homeDirectory();
            Optional<DshAccountRoute.Prepared> prepared = DshAccountRoute.prepare(account);
            if (prepared.isEmpty()) {
                return Handover.NONE;
            }
            DshAccountRoute.Prepared described = prepared.get();
            // Asked every time, as in a launch: a route with no models is refused by the harness,
            // and the vendor's list is the only source for a route it has never heard of.
            described.resolveModels(account);

            // A route an earlier account of this home left in the console profile names a variable
            // nothing sets, so it goes — but only names the launcher builds routes under, exactly
            // the set a launch takes back.
            for (DshAccount known : SettingsManager.settings().getAccounts()) {
                if (known.carriesAKey() && !known.displayName().equals(route)) {
                    DshAccountRoute.remove(home, PROFILE, known.displayName());
                }
            }
            DshAccountRoute.apply(home, PROFILE, described);

            Map<String, String> environment = new LinkedHashMap<>();
            String key = account.apiKey().trim();
            environment.put(DshAccountRoute.environmentVariable(route), key);
            if (described.deepSeek()) {
                // The harness's own web search reads this one, and only DeepSeek's service
                // accepts DeepSeek's key — the same single case a launch sets it for.
                environment.put(DshAccountRoute.WEB_SEARCH_ENVIRONMENT_VARIABLE, key);
            }
            written.put(instance.id(), route);

            // An account of the launcher's own vendor needs no default model: the harness knows that
            // catalogue and picks from it. Any other vendor's route has to be named, or the harness
            // starts on its own supplier — which is the one that answers "no API key" for a key that
            // is not its vendor's. Same rule, same file, and left alone when it already says so.
            boolean defaultModel = false;
            if (account.kind() != DshAccount.AccountKind.OFFICIAL) {
                try {
                    defaultModel = DshDefaultModel.apply(home, route, account.modelOrDefault());
                } catch (DshException e) {
                    LOG.warning("[acp] could not set the default model for the console of "
                            + instance.id(), e);
                }
            }
            return new Handover(Map.copyOf(environment), route, defaultModel);
        } catch (DshException | RuntimeException e) {
            LOG.warning("[acp] could not hand account " + route + " to the console of " + instance.id(), e);
            written.remove(instance.id());
            return Handover.NONE;
        }
    }

}
