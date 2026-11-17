/*
 * HDSL
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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Turns an [DshInstance] into the exact command and environment to run.
///
/// The launcher always invokes the version's own `lib/bin.js` with `node`
/// rather than a `dsh` shim on `PATH`. That is what pins a profile to a
/// specific installation: DeepSeek Harness resolves its own bundles from the
/// installation that booted it.
///
/// `DSH_*` variables are written into the child's environment rather than into
/// a `.env` file, because upstream refuses to read `DSH_`-prefixed names from
/// `.env` files.
@NotNullByDefault
public final class DshLauncher {
    private DshLauncher() {
    }

    /// The complete plan for one launch.
    ///
    /// @param instance         the instance being launched
    /// @param version          the pinned version
    /// @param surface          the surface derived from the instance's profile
    /// @param command          the program and its arguments, in order
    /// @param workingDirectory the directory sessions are scoped to
    /// @param environment      the child environment
    /// @param homeDirectory    the `DSH_HOME` the child is given
    public record LaunchPlan(
            DshInstance instance,
            DshSurface surface,
            @Unmodifiable List<String> command,
            Path workingDirectory,
            @Unmodifiable Map<String, String> environment,
            Path homeDirectory,
            int port) {

        /// Returns the port the browser surface binds.
        ///
        /// @return the port, or `0` for a non-web surface
        public int port() {
            return port;
        }

        /// Renders the plan as a single shell-ready line, for logs and bug reports.
        ///
        /// @return the command line
        public String commandLine() {
            StringBuilder builder = new StringBuilder();
            builder.append("DSH_HOME=").append(homeDirectory).append(' ');
            for (String part : command) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(part.indexOf(' ') >= 0 ? '"' + part + '"' : part);
            }
            return builder.toString();
        }
    }

    /// Resolves the Node runtime an instance is pinned to.
    ///
    /// A system runtime is rejected when it falls outside DeepSeek Harness's
    /// `engines.node` range, because the failure would otherwise surface much
    /// later as an obscure startup error inside the child process.
    ///
    /// @param instance the instance
    /// @return the resolved runtime
    /// @throws DshException when the selected runtime is missing or unsupported
    public static DshNodeRuntime resolveRuntime(DshInstance instance) throws DshException {
        return resolveRuntime(instance, false);
    }

    /// Resolves the runtime to use for work that runs `pnpm`: plugins, `dsh plugin` resolves.
    ///
    /// The difference shows up on a machine whose `PATH` has a Node.js without pnpm
    /// while a managed runtime — installed *with* pnpm — sits in the data directory.
    /// The instance's own choice is still honoured; only the "whatever PATH offers"
    /// half gets the pnpm-aware order, so an instance pinned to a managed runtime
    /// keeps using that one (and, if that one has no pnpm, is told so).
    ///
    /// @param instance the instance
    /// @return the resolved runtime
    /// @throws DshException when the selected runtime is missing or unsupported
    public static DshNodeRuntime resolveRuntimeForPnpm(DshInstance instance) throws DshException {
        return resolveRuntime(instance, true);
    }

    /// Hands an instance's own directories to the agent before anything runs in them.
    ///
    /// The panel creates `instances/<id>/` and its `home/` as root; the harness, its `pnpm`
    /// installs and everything it writes afterwards run as the dropped uid. On a bare host
    /// that is invisible (`/data` is the same user for both), and inside the container the
    /// setgid volume papers over it — until the data directory is a plain bind mount, and then
    /// a harness that cannot write `DSH_HOME` fails in ways that look like the harness's own
    /// bugs. Same rule as a brand instance (see `BrandRuntime#handInstanceDirsToAgent`).
    ///
    /// Only paths inside the panel's own data root are touched: an instance may point its home
    /// or workspace at the operator's directory, and changing the owner of that would be
    /// changing their files.
    ///
    /// Idempotent, so it runs before every launch and heals an instance created by an older
    /// panel.
    ///
    /// @param instance the instance about to be launched
    /// @throws DshException when the instance's own paths cannot be resolved
    static void prepareInstanceDirectories(DshInstance instance) throws DshException {
        if (!Privileges.dropsChildren()) {
            return;
        }
        Path root = DshPaths.ROOT.toAbsolutePath().normalize();
        for (Path dir : List.of(instance.instanceDirectory(), instance.homeDirectory())) {
            Path normalized = dir.toAbsolutePath().normalize();
            if (normalized.startsWith(root)) {
                Privileges.prepareAgentDirectory(normalized, Privileges.AGENT_SHARED_DIRECTORY);
            }
        }
        // Those two directories are all an empty instance has. A used one also has `profiles/`,
        // `storages/` and `settings.yaml`, and a home written by an older panel — or by the `--help`
        // probe before it ran as the agent — has them root-owned: chowning the top directory heals
        // nothing there, and the harness dies writing the profile it composes beside them
        // (`EACCES …/profiles/web/cordis.yml`). Hand the whole tree over once, when one of the names
        // a harness writes is still somebody else's; a healthy home pays a stat per name, not a walk.
        Path home = instance.homeDirectory().toAbsolutePath().normalize();
        if (home.startsWith(root) && hasForeignEntries(home, instance.profile())) {
            int handed = Privileges.handTreeToAgent(home);
            LOG.info("Handed " + handed + " entries of " + home + " to uid " + Privileges.AGENT_UID
                    + " so the harness can write its own profile");
        }
    }

    /// Reports whether a home still holds an entry the harness has to write that belongs to somebody else.
    ///
    /// The names are the ones a harness rewrites — the profile directory and its patch layer, the
    /// settings it composes, the storages it appends to — plus the home itself, which is where an
    /// older panel's `docker cp` or a root-run `dsh` leaves its mark.
    ///
    /// @param home    the instance's `DSH_HOME`
    /// @param profile the profile the instance boots
    /// @return whether a hand-over of the whole tree is worth the walk
    private static boolean hasForeignEntries(Path home, String profile) {
        for (String name : List.of("", "profiles", "profiles/" + profile,
                "profiles/" + profile + "/cordis.patch.yml", "storages", "sessions",
                "attachments", "settings.yaml", ".env")) {
            Path entry = name.isEmpty() ? home : home.resolve(name);
            if (!Files.exists(entry)) {
                continue;
            }
            int owner = Privileges.ownerOf(entry);
            if (owner >= 0 && owner != Privileges.AGENT_UID) {
                return true;
            }
        }
        return false;
    }

    /// The resolution itself, with "this work needs pnpm" as the flag.
    ///
    /// @param instance the instance
    /// @param needsPnpm whether to prefer a runtime that carries `pnpm`
    /// @return the resolved runtime
    /// @throws DshException when the selected runtime is missing or unsupported
    private static DshNodeRuntime resolveRuntime(DshInstance instance, boolean needsPnpm)
            throws DshException {
        String selection = instance.nodeRuntimeOrDefault();
        NodeSelection parsed = NodeSelection.parse(selection);
        if (parsed.kind() == NodeSelection.Kind.AUTO || parsed.kind() == NodeSelection.Kind.INHERIT) {
            // PATH 上没有就走面板托管的运行时（设置 → Node.js 一键安装）——
            // 裸部署没有系统 Node.js 也能工作。
            //
            // 这里要的不是"PATH 上有个 node"，而是"有个能跑的 node"：Debian/Ubuntu 仓库的
            // node 常常低于 22.19，选它只会让启动在这里被拒，而数据目录里那份合用的托管
            // 运行时压根没人问。所以要 pnpm 的路径挑 pnpm，别的挑版本够的。
            DshNodeRuntime runtime = (needsPnpm ? DshNodeRuntime.detectWithPnpm()
                    : DshNodeRuntime.detectSupported())
                    .or(DshNodeRuntime::detectManaged)
                    .orElseThrow(() -> new DshException("Node.js was not found on PATH and no panel-managed "
                            + "runtime is installed (" + DshNodeRuntime.requirement()
                            + "). Open Settings -> Node.js for a one-click managed runtime, "
                            + "or install Node.js yourself."));
            DshVersionManager.requireSupportedNode(runtime);
            return runtime;
        }

        // A channel (`22`, `lts`) or an exact version. The resolver is the same
        // one the Node page uses, so the dropdown and the launch cannot disagree.
        NodeSelection.Resolution resolved = NodeSelection.resolve(parsed, null, needsPnpm);
        DshNodeRuntime runtime = resolved.runtime();
        if (runtime == null) {
            throw new DshException("Node.js " + selection + " 不可用: " + resolved.reason()
                    + "；在「设置 → Node.js」安装它，或把这个实例切到「自动」");
        }
        if (NodeRuntimeManager.findInstalled(runtime.nodeVersion()) != null) {
            NodeRuntimeManager.touch(runtime.nodeVersion());
        }
        return runtime;
    }

    /// How long a version is given to answer a help request.
    private static final java.time.Duration PROBE_TIMEOUT = java.time.Duration.ofSeconds(5);

    /// The help that has been read, keyed by version **and profile**, and whether it mentions
    /// `--no-open`.
    ///
    /// Keyed by both because the answer belongs to both: what is read is
    /// `dsh --profile <name> --help`, and two profiles of one release accept different flags. Cached
    /// by version alone, the first profile to be launched would answer for every other one.
    private static final Map<String, Boolean> NO_OPEN_SUPPORT = new java.util.concurrent.ConcurrentHashMap<>();

    /// Returns the key the help of a version and profile is remembered under.
    ///
    /// @param version the version
    /// @param profile the profile
    /// @return the key
    static String capabilityKey(String version, String profile) {
        return version + "/" + profile;
    }

    /// Reports whether a version's interface accepts `--no-open`.
    ///
    /// Asked of the version itself rather than decided from its number: what a
    /// release accepts is what its own help says, and a list of version numbers that
    /// once needed the flag is a list that goes stale.
    ///
    /// The help that has to be read is the **profile's**, not the launcher's:
    /// `dsh --help` lists the profile selection and the launcher's own flags, and says nothing about
    /// what the booted app accepts. Reading that one always answers "no", which is why every launch
    /// passed no `--no-open` and the harness opened a tab of its own on top of the launcher's —
    /// two tabs for one instance. `dsh --profile <name> --help` is the request the tool documents
    /// for this ("`dsh --profile web -h` prints the web app's help, not this one's").
    ///
    /// @param instance the instance
    /// @return whether the flag may be passed
    private static boolean acceptsNoOpen(DshInstance instance) {
        String version = instance.version();
        String key = capabilityKey(version, instance.profile());
        Boolean cached = NO_OPEN_SUPPORT.get(key);
        if (cached != null) {
            return cached;
        }

        boolean accepted = false;
        Process probe = null;
        try {
            DshNodeRuntime runtime = resolveRuntime(instance);
            Path script = instance.dshEntryPoint();
            // As the agent, not as the panel. A `--help` is not read-only: it composes the profile
            // first, and composing writes `cordis.yml`, `cordis.patch.yml` and `package.json` into
            // `$DSH_HOME/profiles/<profile>`. Run as root — which is what this probe did — those
            // files arrive root-owned, and every later launch, dropping to the agent, fails to
            // rewrite them with `EACCES` that looks like the harness's own bug. Same argv, one
            // wrapper: see `Privileges#asAgent`.
            ProcessBuilder builder = new ProcessBuilder(Privileges.asAgent(
                    List.of(runtime.node().toString(), script.toString(),
                            "--profile", instance.profile(), "--help")));
            builder.redirectErrorStream(true);
            builder.environment().put("DSH_HOME", instance.homeDirectory().toString());
            if (instance.workspacePath() != null) {
                builder.directory(instance.workspacePath().toFile());
            }
            probe = builder.start();

            // Bounded, because what is being asked may not be what a program that only wants to
            // start would answer: a help request that is ignored leaves a process running for as
            // long as it likes, and waiting for it would be waiting for an instance to finish.
            String output;
            try (InputStream stream = probe.getInputStream()) {
                if (!probe.waitFor(PROBE_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)) {
                    // Left out rather than waited for — and remembered, like every other answer:
                    // returning here would ask the same question again on the next launch.
                    LOG.info("DeepSeek Harness " + version + " did not answer --help within "
                            + PROBE_TIMEOUT.toSeconds() + "s; leaving the flag out");
                    NO_OPEN_SUPPORT.put(key, false);
                    return false;
                }
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            accepted = helpMentionsNoOpen(output);
        } catch (DshException | IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Not knowing is not permission: a flag a version does not accept stops it from
            // starting at all, and the cost of leaving it out is a browser tab the harness opens
            // for itself.
            LOG.warning("Could not read the help of DeepSeek Harness " + version, e);
        } finally {
            if (probe != null && probe.isAlive()) {
                probe.destroyForcibly();
            }
        }

        NO_OPEN_SUPPORT.put(key, accepted);
        return accepted;
    }

    /// Reports whether a version's help mentions `--no-open`.
    ///
    /// @param help the help output
    /// @return whether the flag is listed
    static boolean helpMentionsNoOpen(String help) {
        return help != null && help.contains("--no-open");
    }

    /// Builds the launch plan for an instance.
    ///
    /// @param instance the instance to launch
    /// @return the launch plan
    /// @throws DshException when the pinned version or runtime is missing, the
    ///                       entry script is absent, or the workspace cannot be created
    public static LaunchPlan plan(DshInstance instance) throws DshException {
        return plan(instance, null);
    }

    /// Builds the launch plan for an instance, with an account if one was chosen.
    ///
    /// @param instance the instance to launch
    /// @param account  the account to hand the harness, or `null` for none
    /// @return the launch plan
    /// @throws DshException when the pinned version or runtime is missing, the
    ///                       entry script is absent, or the workspace cannot be created
    public static LaunchPlan plan(DshInstance instance, @Nullable DshAccount account)
            throws DshException {
        return plan(instance, account, null);
    }

    /// Builds the launch plan for an instance, with an account whose route is already described.
    ///
    /// A caller that has to show the person what it is doing — the launch dialog, which names each
    /// step as it happens — describes the account itself, asks the supplier for its models, and hands
    /// the result here. A caller with nothing to show passes `null` and the route is written here.
    ///
    /// @param instance  the instance to launch
    /// @param account   the account to hand the harness, or `null` for none
    /// @param prepared  the described route, or `null` to write it in this call
    /// @return the launch plan
    /// @throws DshException when the pinned version or runtime is missing, the
    ///                       entry script is absent, or the workspace cannot be created
    public static LaunchPlan plan(DshInstance instance, @Nullable DshAccount account,
                                  @Nullable DshAccountRoute.Prepared prepared)
            throws DshException {
        prepareInstanceDirectories(instance);
        DshNodeRuntime runtime = resolveRuntime(instance);
        // The instance runs its own copy, so there is nothing to look up: either
        // its copy is there or the instance is not ready to run.
        Path script = instance.dshEntryPoint();
        if (!Files.isRegularFile(script)) {
            throw new DshException("Instance " + instance.id()
                    + " has no DeepSeek Harness of its own; " + script + " is missing");
        }
        // A tree that was written by an interrupted install still has `bin.js`; what it
        // does not have is every package. Refusing it here turns "the agent answers every
        // request with 'waiting for workingDirectory'" into one sentence that says what
        // happened and what to do.
        int empty = org.jackhuang.hmcl.dsh.DshVersionManager.emptyPackages(instance.dshDirectory());
        if (empty > 0) {
            throw new DshException("实例 " + instance.id() + " 的 DeepSeek Harness 安装不完整（"
                    + empty + " 个包目录是空的）：上一次安装被打断了。请在实例页把版本重装一次并等它装完；"
                    + "用这棵树启动只会得到一串 'waiting for workingDirectory'。");
        }

        Path workspace = instance.workspacePath();
        try {
            Files.createDirectories(workspace);
        } catch (java.io.IOException e) {
            throw new DshException("The working directory " + workspace + " cannot be created", e);
        }

        Path home = instance.homeDirectory();
        if (instance.homeMode() == DshHomeMode.ISOLATED) {
            try {
                Files.createDirectories(home);
            } catch (java.io.IOException e) {
                throw new DshException("The instance home " + home + " cannot be created", e);
            }
        }

        // From what the profile boots, not from what it is called: a pack's profile is named after
        // the pack and still boots the browser app.
        DshSurface surface = DshSurface.of(instance);

        // The port is settled here rather than left to the child, and it is the
        // same one on every launch of this instance.
        int port = surface.isWeb() ? DshPorts.resolve(instance) : 0;

        // What the instance runs with, as the user typed it. Its launcher flags go before the
        // profile and its app flags after; `--port` and `DSH_HOME` are refused and reported. Read
        // first because which profile is booted decides which profile every file below belongs to.
        DshLaunchArguments.Parsed typed = DshLaunchArguments.parseArguments(instance.extraArguments());
        String profile = typed.profile() != null ? typed.profile() : instance.profile();

        // Anything a launch that was killed before it could tidy up left behind, put back first: the
        // note beside the harness's own settings says the last launch never got to its own cleanup,
        // so it is dealt with here — **before** this launch decides what to inject, and whatever kind
        // of launch this is: one with no account tidies up after one that had a key, which is what
        // keeps a supplier out of a launch that wants none.
        // Every name the launcher builds routes under: the accounts that carry a key, which are the
        // ones it makes a supplier for.
        java.util.List<String> accountRoutes = new java.util.ArrayList<>();
        for (DshAccount known : org.jackhuang.hmcl.setting.SettingsManager.settings().getAccounts()) {
            if (known.carriesAKey()) {
                accountRoutes.add(known.displayName());
            }
        }
        // And the route this launch itself will build, if it builds one.
        String injecting = account != null && account.carriesAKey() ? account.displayName() : null;

        try {
            DshInjectedSettings.settle(instance);
            // Then the launcher's own work from launches that are long over, taken away without
            // asking: a route left behind is a supplier the harness offers with nothing behind it,
            // and a launch that wants no supplier must not inherit one.
            DshInjectedSettings.clean(instance, profile, accountRoutes, injecting);
        } catch (DshException e) {
            LOG.warning("Could not put back what the last launch of " + instance.id()
                    + " left in its settings", e);
        }

        // And the same contract in the home's own .env, which outlives this launch: a
        // person who starts the harness by hand — or from the terminal this launcher opens
        // on it — gets the answers a launcher's own launch gives. Not fatal when it cannot
        // be written: this run already carries the values in its environment.
        try {
            DshAccountContract.publish(instance, account);
        } catch (DshException e) {
            LOG.warning("Could not write the account contract into the home of " + instance.id(), e);
        }

        // What this launch is about to disturb, noted before it does. Written here because it lives
        // exactly as long as the launch does.
        if (account != null && account.carriesAKey()) {
            DshInjectedSettings.capture(instance, account.displayName(), account.key(), profile);
            // Then the profile object, so the route is one the harness's own models page can see and
            // edit while it runs. The note written just above is what takes it away again — which is
            // why this comes second and not before.
            try {
                DshInjectedSettings.publish(instance, account.displayName(),
                        DshAccountRoute.environmentVariable(account.displayName()));
            } catch (DshException e) {
                // Not fatal: the route still works, it is only not on that page.
                LOG.warning("Could not make " + account.displayName() + " visible on the models page", e);
            }
        }

        // The account's supplier, as a route in the profile's **own** patch layer — the file the
        // harness's own configuration editor writes. Not a `--patch` overlay: an overlay is applied
        // over that file and replaces the entry's whole config, which is what made adding a supplier
        // impossible and hid the person's own suppliers. It stays there for this launch only; the
        // note captured above is what takes it back out. See DshAccountRoute.
        //
        // A caller that described the route already — so it could say so while the supplier is asked
        // — wrote its own copy, and is not asked a second time.
        DshAccountRoute.Prepared route = prepared;
        if (route == null && account != null && account.carriesAKey()) {
            route = DshAccountRoute.prepare(account).orElse(null);
            if (route != null) {
                route.resolveModels(account);
            }
        }
        if (route != null) {
            DshAccountRoute.apply(home, profile, route);
        }

        // A route says what the harness is offered; it cannot make the harness *use* one, because
        // the person's own answer outranks a route the launcher wrote. So the default model is
        // written where that answer lives — and, when the account names no model, not written at all.
        //
        // The provider written here is the **account's name**, which is the route name just created.
        // It used to be the vendor's id, which stopped being the route name when routes became the
        // person's own suppliers — and a default naming a route that does not exist is a harness that
        // will not start.
        //
        // For the launcher's own vendor there is nothing to write either way: the harness knows that
        // vendor's catalogue and picks from it, so naming a model would be naming one of a list it
        // can already read.
        if (account != null && account.carriesAKey()
                && account.kind() != DshAccount.AccountKind.OFFICIAL) {
            try {
                DshDefaultModel.apply(home, account.displayName(), account.modelOrDefault());
            } catch (DshException e) {
                // Not fatal: the route is still there and can be chosen by hand. What must not
                // happen is a launch that does not start because a convenience could not be set.
                LOG.warning("Could not set the default model for " + instance.id(), e);
            }
        }

        List<String> command = new ArrayList<>();
        command.add(runtime.node().toString());
        command.add(script.toString());
        command.addAll(typed.launcherArguments());
        // The profile is stated after the user's launcher flags, so one they named wins; when they
        // named none this is the instance's own.
        command.add("--profile");
        command.add(profile);

        List<String> surfaceArguments = new ArrayList<>(surface.arguments(port));
        // Whether to leave the browser alone is the user's to decide once they have said anything
        // at all about how the instance starts: a line that mentions `--no-open` gets it and one
        // that does not gets nothing, so the harness opens the tab itself and the launcher does not
        // add a second one. With no line typed the launcher asks the version whether it knows the
        // flag, because passing one an old release rejects is what stops a launch outright.
        boolean noOpen = surfaceArguments.remove("--no-open")
                && (typed.appArguments().isEmpty()
                        ? acceptsNoOpen(instance)
                        : typed.asksNoOpen());
        if (noOpen) {
            surfaceArguments.add("--no-open");
        }
        command.addAll(surfaceArguments);
        // The app arguments as typed. A `--no-open` in them belongs to the app and stays where the
        // user put it, which is why it is not filtered out here.
        command.addAll(typed.appArguments());

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DSH_HOME", home.toString());
        // The key travels here and nowhere else: an inherited variable is the highest-precedence
        // source the harness reads, and it is gone when the process is. One variable per route, so
        // that a route left in the profile by another account names a variable nothing sets rather
        // than picking this key up. See DshAccountRoute.
        if (account != null && account.carriesAKey()) {
            String key = account.apiKey().trim();
            environment.put(DshAccountRoute.environmentVariable(account.displayName()), key);
            // And the harness's own web search, which reads a name of the vendor's rather than a
            // route's — and only for a DeepSeek route, because the service behind that entry is
            // DeepSeek's and another vendor's key would be refused by it. The entry itself is not
            // touched: the launcher used to patch it, which made it unsavable in the settings page
            // for exactly the reason the supplier route was.
            if (route != null && route.deepSeek()) {
                environment.put(DshAccountRoute.WEB_SEARCH_ENVIRONMENT_VARIABLE, key);
            }
        }
        environment.putAll(runtime.pathEnvironment());
        environment.putAll(DshEnvironment.of(instance));
        // What a plugin needs to show who this instance is running as. Last, so the
        // contract states what is rather than what somebody typed: it is a description of
        // this launch, and a description that can be quietly overwritten is a lie waiting
        // to happen. See DshAccountContract for the same values in the home's .env.
        environment.putAll(DshAccountContract.values(instance, account));

        LOG.debug("Launching " + instance.id() + " with the command: " + String.join(" ", command));
        return new LaunchPlan(instance, surface, List.copyOf(command), workspace,
                Map.copyOf(environment), home, port);
    }
}
