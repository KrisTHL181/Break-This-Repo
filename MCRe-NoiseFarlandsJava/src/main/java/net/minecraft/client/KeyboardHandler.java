package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeatureCountTracker;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class KeyboardHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DEBUG_CRASH_TIME = 10000;
    private final Minecraft minecraft;
    private final ClipboardManager clipboardManager = new ClipboardManager();
    private long debugCrashKeyTime = -1L;
    private long debugCrashKeyReportedTime = -1L;
    private long debugCrashKeyReportedCount = -1L;
    private boolean usedDebugKeyAsModifier;
    private @Nullable PreeditEvent lastPreeditEvent;

    public KeyboardHandler(final Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    private boolean handleChunkDebugKeys(final KeyEvent event) {
        return switch (event.key()) {
            case 69 -> {
                if (this.minecraft.player == null) {
                    yield false;
                } else {
                    boolean chunkSectionPaths = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_PATHS);
                    this.debugFeedback("SectionPath: " + (chunkSectionPaths ? "shown" : "hidden"));
                    yield true;
                }
            }
            case 70 -> {
                boolean fogEnabled = FogRenderer.toggleFog();
                this.debugFeedbackEnabledStatus("Fog: ", fogEnabled);
                yield true;
            }
            default -> false;
            case 76 -> {
                this.minecraft.smartCull = !this.minecraft.smartCull;
                this.debugFeedbackEnabledStatus("SmartCull: ", this.minecraft.smartCull);
                yield true;
            }
            case 79 -> {
                if (this.minecraft.player == null) {
                    yield false;
                } else {
                    boolean renderOctree = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_OCTREE);
                    this.debugFeedbackEnabledStatus("Frustum culling Octree: ", renderOctree);
                    yield true;
                }
            }
            case 85 -> {
                if (event.hasShiftDown()) {
                    this.minecraft.gameRenderer.mainCamera().killFrustum();
                    this.debugFeedback("Killed frustum");
                } else {
                    this.minecraft.gameRenderer.mainCamera().captureFrustum();
                    this.debugFeedback("Captured frustum");
                }

                yield true;
            }
            case 86 -> {
                if (this.minecraft.player == null) {
                    yield false;
                } else {
                    boolean sectionVisibility = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
                    this.debugFeedbackEnabledStatus("SectionVisibility: ", sectionVisibility);
                    yield true;
                }
            }
            case 87 -> {
                this.minecraft.wireframe = !this.minecraft.wireframe;
                this.debugFeedbackEnabledStatus("WireFrame: ", this.minecraft.wireframe);
                yield true;
            }
        };
    }

    private void debugFeedbackEnabledStatus(final String prefix, final boolean isEnabled) {
        this.debugFeedback(prefix + (isEnabled ? "enabled" : "disabled"));
    }

    private static Component decorateDebugComponent(final ChatFormatting formatting, final Component component) {
        return Component.empty()
            .append(Component.translatable("debug.prefix").withStyle(formatting, ChatFormatting.BOLD))
            .append(CommonComponents.SPACE)
            .append(component);
    }

    private void debugWarningComponent(final Component component) {
        this.minecraft.showDebugChat(decorateDebugComponent(ChatFormatting.RED, component));
    }

    private void debugFeedbackComponent(final Component component) {
        this.minecraft.showDebugChat(decorateDebugComponent(ChatFormatting.YELLOW, component));
    }

    private void debugFeedbackTranslated(final String pattern) {
        this.debugFeedbackComponent(Component.translatable(pattern));
    }

    private void debugFeedback(final String message) {
        this.debugFeedbackComponent(Component.literal(message));
    }

    private boolean handleDebugKeys(final KeyEvent event) {
        if (this.debugCrashKeyTime > 0L && this.debugCrashKeyTime < Util.getMillis() - 100L) {
            return true;
        }

        if (SharedConstants.DEBUG_HOTKEYS && this.handleChunkDebugKeys(event)) {
            return true;
        }

        if (SharedConstants.DEBUG_FEATURE_COUNT) {
            switch (event.key()) {
                case 76:
                    FeatureCountTracker.logCounts();
                    return true;
                case 82:
                    FeatureCountTracker.clearCounts();
                    return true;
            }
        }

        Options options = this.minecraft.options;
        boolean debugAction = false;
        if (options.keyDebugReloadChunk.matches(event)) {
            this.minecraft.levelExtractor.allChanged();
            this.debugFeedbackTranslated("debug.reload_chunks.message");
            debugAction = true;
        }

        if (options.keyDebugShowHitboxes.matches(event) && this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
            boolean renderHitBoxes = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.ENTITY_HITBOXES);
            this.debugFeedbackTranslated(renderHitBoxes ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
            debugAction = true;
        }

        if (options.keyDebugClearChat.matches(event)) {
            this.minecraft.gui.hud.getChat().clearMessages(false);
            debugAction = true;
        }

        if (options.keyDebugShowChunkBorders.matches(event) && this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
            boolean displayChunkborder = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_BORDERS);
            this.debugFeedbackTranslated(displayChunkborder ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            debugAction = true;
        }

        if (options.keyDebugShowAdvancedTooltips.matches(event)) {
            options.advancedItemTooltips = !options.advancedItemTooltips;
            this.debugFeedbackTranslated(options.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
            options.save();
            debugAction = true;
        }

        if (options.keyDebugCopyRecreateCommand.matches(event)) {
            if (this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
                this.copyRecreateCommand(this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), !event.hasShiftDown());
            }

            debugAction = true;
        }

        if (options.keyDebugSpectate.matches(event)) {
            if (this.minecraft.player == null || !GameModeCommand.PERMISSION_CHECK.check(this.minecraft.player.permissions())) {
                this.debugFeedbackTranslated("debug.creative_spectator.error");
            } else if (!this.minecraft.player.isSpectator()) {
                this.minecraft.player.connection.send(new ServerboundChangeGameModePacket(GameType.SPECTATOR));
            } else {
                GameType newGameType = MoreObjects.firstNonNull(this.minecraft.gameMode.getPreviousPlayerMode(), GameType.CREATIVE);
                this.minecraft.player.connection.send(new ServerboundChangeGameModePacket(newGameType));
            }

            debugAction = true;
        }

        if (options.keyDebugSwitchGameMode.matches(event) && this.minecraft.level != null && this.minecraft.gui.screen() == null) {
            if (this.minecraft.canSwitchGameMode() && GameModeCommand.PERMISSION_CHECK.check(this.minecraft.player.permissions())) {
                this.minecraft.gui.setScreen(new GameModeSwitcherScreen());
            } else {
                this.debugFeedbackTranslated("debug.gamemodes.error");
            }

            debugAction = true;
        }

        if (options.keyDebugDebugOptions.matches(event)) {
            if (this.minecraft.gui.screen() instanceof DebugOptionsScreen) {
                this.minecraft.gui.screen().onClose();
            } else if (this.minecraft.canInterruptScreen()) {
                if (this.minecraft.gui.screen() != null) {
                    this.minecraft.gui.screen().onClose();
                }

                this.minecraft.gui.setScreen(new DebugOptionsScreen());
            }

            debugAction = true;
        }

        if (options.keyDebugFocusPause.matches(event)) {
            options.pauseOnLostFocus = !options.pauseOnLostFocus;
            options.save();
            this.debugFeedbackTranslated(options.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
            debugAction = true;
        }

        if (options.keyDebugDumpDynamicTextures.matches(event)) {
            Path gameDirectory = this.minecraft.gameDirectory.toPath().toAbsolutePath();
            Path debugTexturePath = TextureUtil.getDebugTexturePath(gameDirectory);
            this.minecraft.getTextureManager().dumpAllSheets(debugTexturePath);
            Component pathComponent = Component.literal(gameDirectory.relativize(debugTexturePath).toString())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(debugTexturePath)));
            this.debugFeedbackComponent(Component.translatable("debug.dump_dynamic_textures", pathComponent));
            debugAction = true;
        }

        if (options.keyDebugReloadResourcePacks.matches(event)) {
            this.debugFeedbackTranslated("debug.reload_resourcepacks.message");
            this.minecraft.reloadResourcePacks();
            debugAction = true;
        }

        if (options.keyDebugProfiling.matches(event)) {
            if (this.minecraft.debugClientMetricsStart(this::debugFeedbackComponent)) {
                this.debugFeedbackComponent(
                    Component.translatable(
                        "debug.profiling.start", 10, options.keyDebugModifier.getTranslatedKeyMessage(), options.keyDebugProfiling.getTranslatedKeyMessage()
                    )
                );
            }

            debugAction = true;
        }

        if (options.keyDebugCopyLocation.matches(event) && this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
            this.debugFeedbackTranslated("debug.copy_location.message");
            this.setClipboard(
                String.format(
                    Locale.ROOT,
                    "/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
                    this.minecraft.player.level().dimension().identifier(),
                    this.minecraft.player.getX(),
                    this.minecraft.player.getY(),
                    this.minecraft.player.getZ(),
                    this.minecraft.player.getYRot(),
                    this.minecraft.player.getXRot()
                )
            );
            debugAction = true;
        }

        if (options.keyDebugDumpVersion.matches(event)) {
            this.debugFeedbackTranslated("debug.version.header");
            VersionCommand.dumpVersion(this.minecraft::showDebugChat);
            debugAction = true;
        }

        if (options.keyDebugPofilingChart.matches(event)) {
            this.minecraft.getDebugOverlay().toggleProfilerChart();
            debugAction = true;
        }

        if (options.keyDebugFpsCharts.matches(event)) {
            this.minecraft.getDebugOverlay().toggleFpsCharts();
            debugAction = true;
        }

        if (options.keyDebugNetworkCharts.matches(event)) {
            this.minecraft.getDebugOverlay().toggleNetworkCharts();
            debugAction = true;
        }

        if (options.keyDebugLightmapTexture.matches(event)) {
            this.minecraft.getDebugOverlay().toggleLightmapTexture();
            debugAction = true;
        }

        return debugAction;
    }

    private void copyRecreateCommand(final boolean addNbt, final boolean pullFromServer) {
        HitResult hitResult = this.minecraft.hitResult;
        if (hitResult != null) {
            switch (hitResult.getType()) {
                case BLOCK:
                    BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
                    Level level = this.minecraft.player.level();
                    BlockState state = level.getBlockState(blockPos);
                    if (addNbt) {
                        if (pullFromServer) {
                            this.minecraft.player.connection.getDebugQueryHandler().queryBlockEntityTag(blockPos, tagx -> {
                                this.copyCreateBlockCommand(state, blockPos, tagx);
                                this.debugFeedbackTranslated("debug.inspect.server.block");
                            });
                        } else {
                            BlockEntity blockEntity = level.getBlockEntity(blockPos);
                            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata(level.registryAccess()) : null;
                            this.copyCreateBlockCommand(state, blockPos, tag);
                            this.debugFeedbackTranslated("debug.inspect.client.block");
                        }
                    } else {
                        this.copyCreateBlockCommand(state, blockPos, null);
                        this.debugFeedbackTranslated("debug.inspect.client.block");
                    }
                    break;
                case ENTITY:
                    Entity entity = ((EntityHitResult)hitResult).getEntity();
                    Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    if (addNbt) {
                        if (pullFromServer) {
                            this.minecraft.player.connection.getDebugQueryHandler().queryEntityTag(entity.getId(), tagx -> {
                                this.copyCreateEntityCommand(id, entity.position(), tagx);
                                this.debugFeedbackTranslated("debug.inspect.server.entity");
                            });
                        } else {
                            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
                                TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                                entity.saveWithoutId(output);
                                this.copyCreateEntityCommand(id, entity.position(), output.buildResult());
                            }

                            this.debugFeedbackTranslated("debug.inspect.client.entity");
                        }
                    } else {
                        this.copyCreateEntityCommand(id, entity.position(), null);
                        this.debugFeedbackTranslated("debug.inspect.client.entity");
                    }
            }
        }
    }

    private void copyCreateBlockCommand(final BlockState state, final BlockPos blockPos, final @Nullable CompoundTag entityTag) {
        StringBuilder description = new StringBuilder(BlockStateParser.serialize(state));
        if (entityTag != null) {
            description.append(entityTag);
        }

        String command = String.format(Locale.ROOT, "/setblock %d %d %d %s", blockPos.getX(), blockPos.getY(), blockPos.getZ(), description);
        this.setClipboard(command);
    }

    private void copyCreateEntityCommand(final Identifier id, final Vec3 pos, final @Nullable CompoundTag entityTag) {
        String command;
        if (entityTag != null) {
            entityTag.remove("UUID");
            entityTag.remove("Pos");
            String snbt = NbtUtils.toPrettyComponent(entityTag).getString();
            command = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", id, pos.x, pos.y, pos.z, snbt);
        } else {
            command = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", id, pos.x, pos.y, pos.z);
        }

        this.setClipboard(command);
    }

    private void keyPress(final long handle, final @KeyEvent.Action int action, final KeyEvent event) {
        Window window = this.minecraft.getWindow();
        if (handle == window.handle()) {
            this.minecraft.getFramerateLimitTracker().onInputReceived();
            Options options = this.minecraft.options;
            boolean modifierAndOverlayIsSame = options.keyDebugModifier.key.getValue() == options.keyDebugOverlay.key.getValue();
            boolean debugModifierDown = options.keyDebugModifier.isDown();
            boolean debugCrash = !options.keyDebugCrash.isUnbound()
                && InputConstants.isKeyDown(this.minecraft.getWindow(), options.keyDebugCrash.key.getValue());
            if (this.debugCrashKeyTime > 0L) {
                if (!debugCrash || !debugModifierDown) {
                    this.debugCrashKeyTime = -1L;
                }
            } else if (debugCrash && debugModifierDown) {
                this.usedDebugKeyAsModifier = modifierAndOverlayIsSame;
                this.debugCrashKeyTime = Util.getMillis();
                this.debugCrashKeyReportedTime = Util.getMillis();
                this.debugCrashKeyReportedCount = 0L;
            }

            Screen screen = this.minecraft.gui.screen();
            if (screen != null) {
                switch (event.key()) {
                    case 258:
                        this.minecraft.setLastInputType(InputType.KEYBOARD_TAB);
                    case 259:
                    case 260:
                    case 261:
                    default:
                        break;
                    case 262:
                    case 263:
                    case 264:
                    case 265:
                        this.minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
                }
            }

            if (action != 1
                || screen instanceof KeyBindsScreen keyBindsScreen && keyBindsScreen.lastKeySelection > Util.getMillis() - 20L
                || !this.minecraft.handleGlobalKeyPress(InputConstants.getKey(event), event.hasControlDownWithQuirk())) {
                if (action != 0) {
                    boolean hasNoEditboxFocused = screen == null
                        || !(screen.getFocused() instanceof EditBox)
                        || !((EditBox)screen.getFocused()).canConsumeInput();
                    if (hasNoEditboxFocused) {
                        if (event.hasControlDownWithQuirk() && event.key() == 66 && this.minecraft.getNarrator().isActive() && options.narratorHotkey().get()) {
                            boolean wasDisabled = options.narrator().get() == NarratorStatus.OFF;
                            options.narrator().set(NarratorStatus.byId(options.narrator().get().getId() + 1));
                            options.save();
                            if (screen != null) {
                                screen.updateNarratorStatus(wasDisabled);
                            }
                        }

                        LocalPlayer var21 = this.minecraft.player;
                    }
                }

                if (screen != null) {
                    try {
                        if (action != 1 && action != 2) {
                            if (action == 0 && screen.keyReleased(event)) {
                                if (options.keyDebugModifier.matches(event)) {
                                    this.usedDebugKeyAsModifier = false;
                                }

                                return;
                            }
                        } else {
                            screen.afterKeyboardAction();
                            if (screen.keyPressed(event)) {
                                if (this.minecraft.gui.screen() == null) {
                                    InputConstants.Key key = InputConstants.getKey(event);
                                    KeyMapping.set(key, false);
                                }

                                return;
                            }
                        }
                    } catch (Throwable t) {
                        CrashReport report = CrashReport.forThrowable(t, "keyPressed event handler");
                        screen.fillCrashDetails(report);
                        CrashReportCategory keyDetails = report.addCategory("Key");
                        keyDetails.setDetail("Key", event.key());
                        keyDetails.setDetail("Scancode", event.scancode());
                        keyDetails.setDetail("Mods", event.modifiers());
                        throw new ReportedException(report);
                    }
                }

                InputConstants.Key key = InputConstants.getKey(event);
                boolean handlesGameInput = this.minecraft.gui.screen() == null;
                boolean handlesGlobalInput = handlesGameInput
                    || this.minecraft.gui.screen() instanceof PauseScreen pauseScreen && !pauseScreen.showsPauseMenu()
                    || this.minecraft.gui.screen() instanceof GameModeSwitcherScreen;
                if (modifierAndOverlayIsSame && options.keyDebugModifier.matches(event) && action == 0) {
                    if (this.usedDebugKeyAsModifier) {
                        this.usedDebugKeyAsModifier = false;
                    } else {
                        this.minecraft.debugEntries.toggleDebugOverlay();
                    }
                } else if (!modifierAndOverlayIsSame && options.keyDebugOverlay.matches(event) && action == 1) {
                    this.minecraft.debugEntries.toggleDebugOverlay();
                }

                if (action == 0) {
                    KeyMapping.set(key, false);
                } else {
                    boolean didDebugAction = false;
                    if (handlesGlobalInput && event.isEscape()) {
                        this.minecraft.pauseGame(debugModifierDown);
                        didDebugAction = debugModifierDown;
                    } else if (debugModifierDown) {
                        didDebugAction = this.handleDebugKeys(event);
                        if (didDebugAction && screen instanceof DebugOptionsScreen debugOptionsScreen) {
                            DebugOptionsScreen.OptionList optionList = debugOptionsScreen.getOptionList();
                            if (optionList != null) {
                                optionList.children().forEach(DebugOptionsScreen.AbstractOptionEntry::refreshEntry);
                            }
                        }
                    }

                    if (modifierAndOverlayIsSame) {
                        this.usedDebugKeyAsModifier |= didDebugAction;
                    }

                    if (this.minecraft.getDebugOverlay().showProfilerChart() && !debugModifierDown) {
                        int digit = event.getDigit();
                        if (digit != -1) {
                            this.minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(digit);
                        }
                    }

                    if (handlesGameInput) {
                        if (didDebugAction) {
                            KeyMapping.set(key, false);
                        } else {
                            KeyMapping.set(key, true);
                            KeyMapping.click(key);
                        }
                    } else if (key == options.keyChat.key || key == options.keyCommand.key) {
                        // MCRe：任何界面（主界面/暂停/设置）聊天键都能触发 KeyMapping，聊天框随时可打开
                        KeyMapping.set(key, true);
                        KeyMapping.click(key);
                    } else if (key == options.keyDebugModifier.key) {
                        options.keyDebugModifier.setDown(!didDebugAction);
                    }
                }
            }
        }
    }

    private void charTyped(final long handle, final CharacterEvent event) {
        if (handle == this.minecraft.getWindow().handle()) {
            Screen screen = this.minecraft.gui.screen();
            if (screen != null && this.minecraft.gui.overlay() == null) {
                try {
                    screen.charTyped(event);
                } catch (Throwable t) {
                    CrashReport report = CrashReport.forThrowable(t, "charTyped event handler");
                    screen.fillCrashDetails(report);
                    CrashReportCategory keyDetails = report.addCategory("Key");
                    keyDetails.setDetail("Codepoint", event.codepoint());
                    throw new ReportedException(report);
                }
            }
        }
    }

    private void preeditCallback(final long handle, final @Nullable PreeditEvent event) {
        if (handle == this.minecraft.getWindow().handle()) {
            this.lastPreeditEvent = event;
            Screen screen = this.minecraft.gui.screen();
            if (screen != null && this.minecraft.gui.overlay() == null) {
                submitPreeditEvent(screen, event);
            }
        }
    }

    public void resubmitLastPreeditEvent(final GuiEventListener screen) {
        submitPreeditEvent(screen, this.lastPreeditEvent);
    }

    public static void submitPreeditEvent(final GuiEventListener element, final @Nullable PreeditEvent event) {
        try {
            element.preeditUpdated(event);
        } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "IME pre-edit event handler");
            if (element instanceof Screen screen) {
                screen.fillCrashDetails(report);
            }

            CrashReportCategory keyDetails = report.addCategory("Event");
            keyDetails.setDetail("Contents", () -> String.valueOf(event));
            throw new ReportedException(report);
        }
    }

    public void setup(final Window window) {
        InputConstants.setupKeyboardCallbacks(window, (window1, keysym, scancode, action, mods) -> {
            KeyEvent event = new KeyEvent(keysym, scancode, mods);
            this.minecraft.execute(() -> this.keyPress(window1, action, event));
        }, (window1, codepoint) -> {
            CharacterEvent event = new CharacterEvent(codepoint);
            this.minecraft.execute(() -> this.charTyped(window1, event));
        }, (window1, preeditSize, preeditPtr, blockCount, blockSizesPtr, focusedBlock, caret) -> {
            PreeditEvent event = PreeditEvent.createFromCallback(preeditSize, preeditPtr, blockCount, blockSizesPtr, focusedBlock, caret);
            this.minecraft.execute(() -> this.preeditCallback(window1, event));
        }, window1 -> this.minecraft.textInputManager().notifyIMEChanged());
    }

    public String getClipboard() {
        return this.clipboardManager.getClipboard(this.minecraft.getWindow(), (error, description) -> {
            if (error != 65545) {
                this.minecraft.getWindow().defaultErrorCallback(error, description);
            }
        });
    }

    public void setClipboard(final String clipboard) {
        if (!clipboard.isEmpty()) {
            this.clipboardManager.setClipboard(this.minecraft.getWindow(), clipboard);
        }
    }

    public void tick() {
        if (this.debugCrashKeyTime > 0L) {
            long now = Util.getMillis();
            long remainingTime = 10000L - (now - this.debugCrashKeyTime);
            long reportedTime = now - this.debugCrashKeyReportedTime;
            if (remainingTime < 0L) {
                if (this.minecraft.hasControlDown()) {
                    Blaze3D.youJustLostTheGame();
                }

                String message = "Manually triggered debug crash";
                CrashReport report = new CrashReport("Manually triggered debug crash", new Throwable("Manually triggered debug crash"));
                CrashReportCategory manualCrashDetails = report.addCategory("Manual crash details");
                NativeModuleLister.addCrashSection(manualCrashDetails);
                throw new ReportedException(report);
            }

            if (reportedTime >= 1000L) {
                if (this.debugCrashKeyReportedCount == 0L) {
                    this.debugFeedbackComponent(
                        Component.translatable(
                            "debug.crash.message",
                            this.minecraft.options.keyDebugModifier.getTranslatedKeyMessage(),
                            this.minecraft.options.keyDebugCrash.getTranslatedKeyMessage()
                        )
                    );
                } else {
                    this.debugWarningComponent(Component.translatable("debug.crash.warning", Mth.ceil((float)remainingTime / 1000.0F)));
                }

                this.debugCrashKeyReportedTime = now;
                this.debugCrashKeyReportedCount++;
            }
        }
    }
}