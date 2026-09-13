package net.minecraft.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * MCRe NoiseFarlands：客户端本地命令执行器。
 * 主界面/未连接世界时，聊天框仍可打开并执行本地命令（/help、/quit 等）。
 * 命令注册在类加载时初始化，不依赖服务器连接，随时可用。
 */
@OnlyIn(Dist.CLIENT)
public final class LocalCommandExecutor {
    private static final CommandDispatcher<SharedSuggestionProvider> DISPATCHER = new CommandDispatcher<>();
    private static final SharedSuggestionProvider SOURCE = new LocalSource();
    private static final List<String> COMMAND_NAMES = List.of("help", "say", "quit", "server_debug");
    // MCRe: keep a reference to the local DedicatedServer instance (started via /server_debug start)
    private static volatile net.minecraft.server.dedicated.DedicatedServer runningServer = null;

    private LocalCommandExecutor() {
    }

    static {
        DISPATCHER.register(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("help").executes(ctx -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("=== 本地命令（未进入世界）===").withStyle(ChatFormatting.GOLD)
                );
                minecraft.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("/help — 显示此帮助").withStyle(ChatFormatting.GRAY)
                );
                minecraft.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("/say <消息> — 在聊天框输出消息").withStyle(ChatFormatting.GRAY)
                );
                minecraft.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("/quit — 退出游戏").withStyle(ChatFormatting.GRAY)
                );
                minecraft.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("/server_debug <stop│start> <Options> — 用于调试改版在服务端层的改动").withStyle(ChatFormatting.GRAY)
                );
                return 1;
            })
        );
        // Register the root "/server_debug" command with three sub‑commands:
        //   • /server_debug                →  prints usage help.
        //   • /server_debug stop           →  stops a running DedicatedServer (if any).
        //   • /server_debug start [options] →  launches a server. Options are parsed as
        //         key=value  →  CLI argument if the key is a known Main option, otherwise
        //                         written to "server.properties" before start.
        //         flag       →  same rule, a flag is passed as "--flag" if known, else set to true.
        //   The command also keeps a volatile reference to the running server via
        //   LocalCommandExecutor.runningServer and Main.runningServer.
        DISPATCHER.register(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("server_debug")
                .executes(ctx -> {
                    // Show simple usage when the user only typed "/server_debug"
                    Minecraft mc = Minecraft.getInstance();
                    mc.gui.hud.getChat().addClientSystemMessage(
                        Component.literal("Usage: /server_debug <start|stop> [options]").withStyle(ChatFormatting.YELLOW)
                    );
                    return 1;
                })
                // ------------------- STOP -------------------
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("stop")
                    .executes(ctx -> {
                        Minecraft mc = Minecraft.getInstance();
                        net.minecraft.server.dedicated.DedicatedServer server = runningServer;
                        if (server != null) {
                            server.halt(true);
                            runningServer = null;
                            mc.gui.hud.getChat().addClientSystemMessage(
                                Component.literal("Server stopped.").withStyle(ChatFormatting.GREEN)
                            );
                        } else {
                            mc.gui.hud.getChat().addClientSystemMessage(
                                Component.literal("Server not running.").withStyle(ChatFormatting.RED)
                            );
                        }
                        return 1;
                    })
                )
                // ------------------- START -------------------
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("start")
                    .executes(ctx -> {
                        // No options: start server with default config (headless, no GUI)
                        String[] argsArray = new String[]{"--nogui"};
                        try {
                            // Ensure required files exist with sane defaults
                            java.nio.file.Path propPath = java.nio.file.Paths.get("server.properties");
                            if (!java.nio.file.Files.exists(propPath)) {
                                java.util.Properties empty = new java.util.Properties();
                                try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(propPath)) {
                                    empty.store(out, null);
                                }
                            }
                            java.nio.file.Path eulaPath = java.nio.file.Paths.get("eula.txt");
                            if (!java.nio.file.Files.exists(eulaPath)) {
                                try (java.io.Writer writer = java.nio.file.Files.newBufferedWriter(eulaPath, java.nio.charset.StandardCharsets.UTF_8)) {
                                    writer.write("eula=true\n");
                                }
                            }
                            net.minecraft.server.Main.main(argsArray);
                            runningServer = net.minecraft.server.Main.getRunningServer();
                            Minecraft mc2 = Minecraft.getInstance();
                            mc2.gui.hud.getChat().addClientSystemMessage(
                                Component.literal("Server started.").withStyle(ChatFormatting.GREEN)
                            );
                        } catch (Throwable t) {
                            Minecraft mc3 = Minecraft.getInstance();
                            mc3.gui.hud.getChat().addClientSystemMessage(
                                Component.literal("Server start failed: " + t.getMessage())
                                    .withStyle(ChatFormatting.RED)
                            );
                            t.printStackTrace();
                        }
                        return 1;
                    })
                    .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("options", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String optStr = StringArgumentType.getString(ctx, "options");
                            java.util.List<String> args = new java.util.ArrayList<>();
                            java.util.Properties propUpdates = new java.util.Properties();
                            // Known CLI options accepted by net.minecraft.server.Main
                            java.util.Set<String> knownCli = java.util.Set.of(
                                "nogui", "initSettings", "demo", "bonusChest",
                                "forceUpgrade", "eraseCache", "recreateRegionFiles",
                                "safeMode", "help", "universe", "world", "port",
                                "serverId", "jfrProfile", "pidFile"
                            );
                            if (!optStr.isBlank()) {
                                for (String token : optStr.split("\\s+")) {
                                    if (token.isEmpty()) continue;
                                    int eqIdx = token.indexOf('=');
                                    if (eqIdx > 0) {
                                        String key = token.substring(0, eqIdx);
                                        String value = token.substring(eqIdx + 1);
                                        if (knownCli.contains(key)) {
                                            args.add("--" + key);
                                            args.add(value);
                                        } else {
                                            // Store into server.properties (will be written later)
                                            propUpdates.setProperty(key, value);
                                        }
                                    } else {
                                        // Flag without value
                                        if (knownCli.contains(token)) {
                                            args.add("--" + token);
                                        } else {
                                            // For properties without explicit value we set "true"
                                            propUpdates.setProperty(token, "true");
                                        }
                                    }
                                }
                            }
                            // Write any property updates to server.properties before launching
                            if (!propUpdates.isEmpty()) {
                                try {
                                    java.nio.file.Path propPath = java.nio.file.Paths.get("server.properties");
                                    // Load existing properties if present
                                    java.util.Properties existing = new java.util.Properties();
                                    if (java.nio.file.Files.exists(propPath)) {
                                        try (java.io.InputStream in = java.nio.file.Files.newInputStream(propPath)) {
                                            existing.load(in);
                                        }
                                    }
                                    // Merge updates
                                    existing.putAll(propUpdates);
                                    try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(propPath)) {
                                        existing.store(out, null);
                                    }
                                } catch (Exception e) {
                                    Minecraft mcErr = Minecraft.getInstance();
                                    mcErr.gui.hud.getChat().addClientSystemMessage(
                                        Component.literal("Failed to write server.properties: " + e.getMessage())
                                            .withStyle(ChatFormatting.RED)
                                    );
                                    e.printStackTrace();
                                }
                            }
                            String[] argsArray = args.toArray(new String[0]);
                            // Ensure headless mode via --nogui if not already present
                            if (!java.util.Arrays.asList(argsArray).contains("--nogui")) {
                                java.util.List<String> tmp = new java.util.ArrayList<>(java.util.Arrays.asList(argsArray));
                                tmp.add(0, "--nogui");
                                argsArray = tmp.toArray(new String[0]);
                            }
                            try {
                                // Ensure required files exist with sane defaults
                                java.nio.file.Path propPath = java.nio.file.Paths.get("server.properties");
                                if (!java.nio.file.Files.exists(propPath)) {
                                    java.util.Properties empty = new java.util.Properties();
                                    try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(propPath)) {
                                        empty.store(out, null);
                                    }
                                }
                                java.nio.file.Path eulaPath = java.nio.file.Paths.get("eula.txt");
                                if (!java.nio.file.Files.exists(eulaPath)) {
                                    try (java.io.Writer writer = java.nio.file.Files.newBufferedWriter(eulaPath, java.nio.charset.StandardCharsets.UTF_8)) {
                                        writer.write("eula=true\n");
                                    }
                                }
                                net.minecraft.server.Main.main(argsArray);
                                // Store reference to the running server (Main already set its static field)
                                runningServer = net.minecraft.server.Main.getRunningServer();
                                Minecraft mc2 = Minecraft.getInstance();
                                mc2.gui.hud.getChat().addClientSystemMessage(
                                    Component.literal("Server started.").withStyle(ChatFormatting.GREEN)
                                );
                            } catch (Throwable t) {
                                Minecraft mc3 = Minecraft.getInstance();
                                mc3.gui.hud.getChat().addClientSystemMessage(
                                    Component.literal("Server start failed: " + t.getMessage())
                                        .withStyle(ChatFormatting.RED)
                                );
                                t.printStackTrace();
                            }
                            return 1;
                        })
                    )
                )
        );
    }


    public static CommandDispatcher<SharedSuggestionProvider> getDispatcher() {
        return DISPATCHER;
    }

    public static SharedSuggestionProvider getSource() {
        return SOURCE;
    }

    public static List<String> getCommandNames() {
        return COMMAND_NAMES;
    }

    /** 主界面/未连接时执行本地命令；未知命令显示红色提示 */
    public static void execute(final Minecraft minecraft, final String command) {
        try {
            ParseResults<SharedSuggestionProvider> parse = DISPATCHER.parse(new StringReader(command), SOURCE);
            DISPATCHER.execute(parse);
        } catch (CommandSyntaxException e) {
            minecraft.gui.hud.getChat().addClientSystemMessage(
                Component.literal("未知或不完整的命令：/" + command).withStyle(ChatFormatting.RED)
            );
        }
    }

    /** 最小 SharedSuggestionProvider 实现（主界面无连接/无世界数据） */
    private static final class LocalSource implements SharedSuggestionProvider {
        @Override
        public PermissionSet permissions() {
            return PermissionSet.NO_PERMISSIONS;
        }

        @Override
        public Collection<String> getOnlinePlayerNames() {
            return Collections.emptyList();
        }

        @Override
        public Collection<String> getAllTeams() {
            return Collections.emptyList();
        }

        @Override
        public Stream<Identifier> getAvailableSounds() {
            return Stream.empty();
        }

        @Override
        public CompletableFuture<Suggestions> customSuggestion(final CommandContext<?> context) {
            return Suggestions.empty();
        }

        @Override
        public Set<ResourceKey<Level>> levels() {
            return Collections.emptySet();
        }

        @Override
        public RegistryAccess registryAccess() {
            return RegistryAccess.EMPTY;
        }

        @Override
        public FeatureFlagSet enabledFeatures() {
            return FeatureFlagSet.of();
        }

        @Override
        public CompletableFuture<Suggestions> suggestRegistryElements(
            final ResourceKey<? extends Registry<?>> key,
            final SharedSuggestionProvider.ElementSuggestionType elements,
            final SuggestionsBuilder builder,
            final CommandContext<?> context
        ) {
            return builder.buildFuture();
        }
    }
}
