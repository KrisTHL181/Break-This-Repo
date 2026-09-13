package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.InputType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreens;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Gui {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
    public static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
    private final Minecraft minecraft;
    public final Hud hud;
    private final GuiRenderState guiRenderState;
    private @Nullable Screen screen;
    private @Nullable Overlay overlay;
    private boolean clientLevelTeardownInProgress;
    private final SplashManager splashManager;
    private final ToastManager toastManager;
    private final ChatListener chatListener;
    private @Nullable TutorialToast socialInteractionsToast;

    public Gui(final Minecraft minecraft, final Hud hud, final GuiRenderState guiRenderState) {
        this.minecraft = minecraft;
        this.hud = hud;
        this.splashManager = new SplashManager(minecraft.getUser());
        this.toastManager = new ToastManager(minecraft, minecraft.options);
        this.chatListener = new ChatListener(minecraft);
        this.chatListener.setMessageDelay(minecraft.options.chatDelay().get());
        this.guiRenderState = guiRenderState;
    }

    public void registerReloadListeners(final ReloadableResourceManager resourceManager) {
        resourceManager.registerReloadListener(this.splashManager);
        this.hud.registerReloadListeners(resourceManager);
    }

    public void tick() {
        ProfilerFiller profiler = Profiler.get();
        this.chatListener.tick();
        this.hud.tick(this.minecraft.isPaused());
        profiler.push("screen");
        LocalPlayer player = this.minecraft.player;
        if (this.screen != null || player == null) {
            if (this.screen instanceof InBedChatScreen inBedScreen && !player.isSleeping()) {
                inBedScreen.onPlayerWokeUp();
            }
        } else if (player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen(null);
        } else if (player.isSleeping() && this.minecraft.level != null) {
            this.hud.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE, InBedChatScreen::new);
        }

        if (this.screen != null) {
            try {
                this.screen.tick();
            } catch (Throwable t) {
                CrashReport report = CrashReport.forThrowable(t, "Ticking screen");
                this.screen.fillCrashDetails(report);
                throw new ReportedException(report);
            }
        }

        profiler.pop();
        if (this.overlay != null) {
            this.overlay.tick();
        }

        if (!this.minecraft.getDebugOverlay().showDebugScreen()) {
            this.hud.clearCache();
        }
    }

    public void update() {
        this.toastManager.update();

        try {
            if (this.screen != null) {
                this.screen.handleDelayedNarration();
            }
        } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Narrating screen");
            CrashReportCategory category = report.addCategory("Screen details");
            category.setDetail("Screen name", () -> this.screen.getClass().getCanonicalName());
            throw new ReportedException(report);
        }
    }

    public void extractRenderState(final DeltaTracker deltaTracker, final boolean shouldRenderLevel, final boolean resourcesLoaded) {
        ProfilerFiller profiler = Profiler.get();
        int xMouse = (int)this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
        int yMouse = (int)this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
        profiler.push("gui");
        this.guiRenderState.reset();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(this.minecraft, this.guiRenderState, xMouse, yMouse);
        if (shouldRenderLevel) {
            profiler.push("hud");
            this.hud.extractRenderState(graphics, deltaTracker);
            profiler.pop();
        }

        if (this.overlay != null) {
            profiler.push("overlay");

            try {
                this.overlay.extractRenderState(graphics, xMouse, yMouse, deltaTracker.getGameTimeDeltaTicks());
            } catch (Throwable t) {
                CrashReport report = CrashReport.forThrowable(t, "Extracting overlay render state");
                CrashReportCategory category = report.addCategory("Overlay details");
                category.setDetail("Overlay name", () -> this.overlay.getClass().getCanonicalName());
                throw new ReportedException(report);
            }

            profiler.pop();
        } else if (resourcesLoaded && this.screen != null) {
            profiler.push("screen");

            try {
                this.screen.extractRenderStateWithTooltipAndSubtitles(graphics, xMouse, yMouse, deltaTracker.getGameTimeDeltaTicks());
            } catch (Throwable t) {
                CrashReport report = CrashReport.forThrowable(t, "Rendering screen");
                CrashReportCategory category = report.addCategory("Screen render details");
                category.setDetail("Screen name", () -> this.screen.getClass().getCanonicalName());
                this.minecraft.mouseHandler.fillMousePositionDetails(category, this.minecraft.getWindow());
                throw new ReportedException(report);
            }

            if (SharedConstants.DEBUG_CURSOR_POS) {
                this.minecraft.mouseHandler.drawDebugMouseInfo(this.minecraft.font, graphics);
            }

            profiler.pop();
        }

        if (shouldRenderLevel) {
            this.hud.extractSavingIndicator(graphics, deltaTracker);
        }

        if (resourcesLoaded) {
            try (Zone ignored = profiler.zone("toasts")) {
                this.toastManager().extractRenderState(graphics);
            }
        }

        if (!(this.screen instanceof DebugOptionsScreen)) {
            this.hud.extractDebugOverlay(graphics);
        }

        this.hud.extractDeferredSubtitles();
        if (SharedConstants.DEBUG_ACTIVE_TEXT_AREAS) {
            this.renderActiveTextDebug();
        }

        profiler.pop();
        graphics.applyCursor(this.minecraft.getWindow());
    }

    @Contract(pure = true)
    public @Nullable Screen screen() {
        return this.screen;
    }

    public void setScreen(@Nullable Screen screen) {
        if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.minecraft.getRunningThread()) {
            LOGGER.error("setScreen called from non-game thread");
        }

        if (this.screen != null) {
            this.screen.removed();
        } else {
            this.minecraft.setLastInputType(InputType.NONE);
        }

        if (screen == null) {
            if (this.clientLevelTeardownInProgress) {
                throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
            }

            if (this.minecraft.level == null) {
                screen = new TitleScreen();
            } else if (this.minecraft.player.isDeadOrDying()) {
                if (this.minecraft.player.shouldShowDeathScreen()) {
                    screen = new DeathScreen(null, this.minecraft.level.getLevelData().isHardcore(), this.minecraft.player);
                } else {
                    this.minecraft.player.respawn();
                }
            } else {
                screen = this.hud.getChat().restoreChatScreen();
            }
        }

        this.screen = screen;
        if (this.screen != null) {
            this.screen.added();
        }

        if (screen != null) {
            this.minecraft.mouseHandler.releaseMouse();
            KeyMapping.releaseAll();
            screen.init(this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
        } else {
            this.minecraft.textInputManager().stopTextInput();
            if (this.minecraft.level != null) {
                KeyMapping.restoreToggleStatesOnScreenClosed();
            }

            this.minecraft.getSoundManager().resume();
            this.minecraft.mouseHandler.grabMouse();
        }

        this.minecraft.updateTitle();
    }

    @Contract(pure = true)
    public @Nullable Overlay overlay() {
        return this.overlay;
    }

    public void setOverlay(final @Nullable Overlay overlay) {
        this.overlay = overlay;
    }

    public boolean isPausing() {
        return this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPausing();
    }

    public SplashManager splashManager() {
        return this.splashManager;
    }

    public ToastManager toastManager() {
        return this.toastManager;
    }

    public ChatListener chatListener() {
        return this.chatListener;
    }

    public void addSocialInteractionsToast() {
        Component title = Component.translatable("tutorial.socialInteractions.title");
        Component message = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
        this.socialInteractionsToast = new TutorialToast(this.minecraft.font, TutorialToast.Icons.SOCIAL_INTERACTIONS, title, message, true, 8000);
        this.toastManager.addToast(this.socialInteractionsToast);
    }

    public void setPauseScreen(final boolean suppressPauseMenuIfWeReallyArePausing, final boolean canGameReallyBePaused) {
        if (this.screen == null) {
            if (canGameReallyBePaused) {
                this.setScreen(new PauseScreen(!suppressPauseMenuIfWeReallyArePausing));
            } else {
                this.setScreen(new PauseScreen(true));
            }
        }
    }

    public void handleKeybinds() {
        Options options = this.minecraft.options;

        while (options.keyToggleGui.consumeClick()) {
            this.hud.toggle();
        }

        while (options.keyAdvancements.consumeClick()) {
            if (this.minecraft.player != null && this.minecraft.player.connection != null) {
                this.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements()));
            }
        }

        while (options.keySocialInteractions.consumeClick()) {
            if (!this.minecraft.isMultiplayerServer() && !SharedConstants.DEBUG_SOCIAL_INTERACTIONS) {
                this.chatListener.handleOverlay(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
                this.minecraft.getNarrator().saySystemNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
            } else {
                if (this.socialInteractionsToast != null) {
                    this.socialInteractionsToast.hide();
                    this.socialInteractionsToast = null;
                }

                this.setScreen(new SocialInteractionsScreen());
            }
        }

        while (options.keyChat.consumeClick()) {
            this.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
        }

        // MCRe：任何界面都能用 / 键打开命令聊天（主界面走本地命令，世界内走服务器命令）
        if (options.keyCommand.consumeClick()) {
            this.openChatScreen(ChatComponent.ChatMethod.COMMAND);
        }
    }

    public void openChatScreen(final ChatComponent.ChatMethod chatMethod) {
        // MCRe：主界面/未连接世界时也可打开聊天（player 为 null 时走本地命令环境）
        this.hud.getChat().openScreen(chatMethod, ChatScreen::new);
    }

    public void openChatAndAddText(final ChatComponent.ChatMethod chatMethod, final String text) {
        this.openChatScreen(ChatComponent.ChatMethod.COMMAND);
        if (this.screen instanceof ChatScreen chatScreen) {
            chatScreen.insertText(text, false);
        }
    }

    public Runnable buildInitialScreens(final @Nullable GameLoadCookie cookie) {
        List<Function<Runnable, Screen>> screens = new ArrayList<>();
        boolean onboardingScreenAdded = this.addInitialScreens(screens);
        Runnable nextStep = () -> {
            if (cookie != null && cookie.quickPlayData().isEnabled()) {
                QuickPlay.connect(this.minecraft, cookie.quickPlayData().variant(), cookie.realmsClient());
            } else {
                this.setScreen(new TitleScreen(true, new LogoRenderer(onboardingScreenAdded)));
            }
        };

        for (Function<Runnable, Screen> function : Lists.reverse(screens)) {
            Screen screen = function.apply(nextStep);
            nextStep = () -> this.setScreen(screen);
        }

        return nextStep;
    }

    private boolean addInitialScreens(final List<Function<Runnable, Screen>> screens) {
        boolean onboardingScreenAdded = false;
        if (this.minecraft.options.onboardAccessibility || SharedConstants.DEBUG_FORCE_ONBOARDING_SCREEN) {
            screens.add(next -> new AccessibilityOnboardingScreen(this.minecraft.options, next));
            onboardingScreenAdded = true;
        }

        BanDetails multiplayerBan = this.minecraft.multiplayerBan();
        if (multiplayerBan != null) {
            screens.add(next -> BanNoticeScreens.create(result -> {
                if (result) {
                    Util.getPlatform().openUri(CommonLinks.SUSPENSION_HELP);
                }

                next.run();
            }, multiplayerBan));
        }

        ProfileResult profileResult = this.minecraft.getProfileResult();
        if (profileResult != null) {
            GameProfile profile = profileResult.profile();
            Set<ProfileActionType> actions = profileResult.actions();
            if (actions.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
                screens.add(onClose -> BanNoticeScreens.createNameBan(profile.name(), onClose));
            }

            if (actions.contains(ProfileActionType.USING_BANNED_SKIN)) {
                screens.add(BanNoticeScreens::createSkinBan);
            }
        }

        return onboardingScreenAdded;
    }

    public boolean canInterruptScreen() {
        return (this.screen == null || this.screen.canInterruptWithAnotherScreen()) && !this.clientLevelTeardownInProgress;
    }

    public void setClientLevelTeardownInProgress(final boolean clientLevelTeardownInProgress) {
        this.clientLevelTeardownInProgress = clientLevelTeardownInProgress;
    }

    private void renderActiveTextDebug() {
        this.guiRenderState.nextStratum();
        this.guiRenderState
            .forEachText(
                text -> text.ensurePrepared()
                    .visit(
                        new Font.GlyphVisitor() {
                            private int index;

                            @Override
                            public void acceptGlyph(final TextRenderable.Styled glyph) {
                                this.renderDebugMarkers(glyph, false);
                            }

                            @Override
                            public void acceptEmptyArea(final EmptyArea empty) {
                                this.renderDebugMarkers(empty, true);
                            }

                            private void renderDebugMarkers(final ActiveArea glyph, final boolean isEmpty) {
                                int intensity = (isEmpty ? 128 : 255) - (this.index++ & 1) * 64;
                                Style style = glyph.style();
                                int red = style.getClickEvent() != null ? intensity : 0;
                                int green = style.getHoverEvent() != null ? intensity : 0;
                                int blue = red != 0 && green != 0 ? 0 : intensity;
                                int color = ARGB.color(128, red, green, blue);
                                Gui.this.guiRenderState
                                    .addGuiElement(
                                        new ColoredRectangleRenderState(
                                            RenderPipelines.GUI,
                                            TextureSetup.noTexture(),
                                            text.pose,
                                            (int)glyph.activeLeft(),
                                            (int)glyph.activeTop(),
                                            (int)glyph.activeRight(),
                                            (int)glyph.activeBottom(),
                                            color,
                                            color,
                                            text.scissor
                                        )
                                    );
                            }
                        }
                    )
            );
    }
}