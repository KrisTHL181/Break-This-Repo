package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.ModMetadata.ModMetadata;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import java.util.Date;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TitleScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("narrator.screen.title");
    private static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
    private static final String DEMO_LEVEL_ID = "Demo_World";
    private @Nullable SplashRenderer splash;
    private @Nullable RealmsNotificationsScreen realmsNotificationsScreen;
    private @Nullable FriendsButton friends;
    private boolean fading;
    private long fadeInStart;
    private final LogoRenderer logoRenderer;

    public TitleScreen() {
        this(false);
    }

    public TitleScreen(final boolean fading) {
        this(fading, null);
    }

    public TitleScreen(final boolean fading, final @Nullable LogoRenderer logoRenderer) {
        super(TITLE);
        this.fading = fading;
        this.logoRenderer = Objects.requireNonNullElseGet(logoRenderer, () -> new LogoRenderer(false));
        this.minecraft.gameRenderer.panorama().startSpin();
    }

    private boolean realmsNotificationsEnabled() {
        return this.realmsNotificationsScreen != null;
    }

    @Override
    public void tick() {
        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.tick();
        }

        if (this.minecraft.getPlayerSocialManager().isFriendListEnabled() && this.friends != null) {
            this.friends.refreshIncomingRequestCount();
        }
    }

    public static void registerTextures(final TextureManager textureManager) {
        textureManager.registerForNextReload(LogoRenderer.MINECRAFT_LOGO);
        textureManager.registerForNextReload(LogoRenderer.MINECRAFT_EDITION);
        textureManager.registerForNextReload(Panorama.PANORAMA_OVERLAY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (this.splash == null) {
            this.splash = this.minecraft.gui.splashManager().getSplash();
        }

        int copyrightWidth = this.font.width(COPYRIGHT_TEXT);
        int copyrightX = this.width - copyrightWidth - 2;
        int spacing = 24;
        int topPos = this.height / 4 + 48;
        if (this.minecraft.isDemo()) {
            topPos = this.createDemoMenuOptions(topPos, 24);
        } else {
            topPos = this.createNormalMenuOptions(topPos, 24);
        }

        int numberOfButtons = 3;
        int currentButton = 0;
        topPos += 24;
        this.friends = this.addRenderableWidget(
                CommonButtons.friends(
                        20,
                        var1x -> OnlineOptionsScreen.confirmFriendsListEnabled(this.minecraft, () -> this.minecraft.gui.setScreen(new FriendsOverlayScreen(this)), this),
                        !this.minecraft.isDemo()
                )
        );
        this.friends.setPosition(this.getHorizontalPosition(++currentButton, 3, 20), topPos);
        SpriteIconButton language = this.addRenderableWidget(
                CommonButtons.language(
                        20, var1x -> this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), true
                )
        );
        language.setPosition(this.getHorizontalPosition(++currentButton, 3, 20), topPos);
        SpriteIconButton accessibility = this.addRenderableWidget(
                CommonButtons.accessibility(20, var1x -> this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true)
        );
        accessibility.setPosition(this.getHorizontalPosition(++currentButton, 3, 20), topPos);
        Button.Builder var10001 = Button.builder(
                Component.translatable("menu.options"), var1x -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, false))
        );
        int var10002 = this.width / 2 - 100;
        topPos += 24;
        this.addRenderableWidget(var10001.bounds(var10002, topPos, 98, 20).build());
        this.addRenderableWidget(
                Button.builder(Component.translatable("menu.quit"), var1x -> this.minecraft.stop()).bounds(this.width / 2 + 2, topPos, 98, 20).build()
        );
        this.addRenderableWidget(
                new PlainTextButton(
                copyrightX,
                this.height - 10,
                copyrightWidth,
                10,
                COPYRIGHT_TEXT,
                var1x -> this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)),
                this.font
                )
        );
        if (this.realmsNotificationsScreen == null) {
            this.realmsNotificationsScreen = new RealmsNotificationsScreen();
        }

        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.init(this.width, this.height);
        }
    }

    private int getHorizontalPosition(final int currentButton, final int numberOfButtons, final int buttonWidth) {
        int totalWidth = numberOfButtons * buttonWidth + (numberOfButtons - 1) * 4;
        return this.width / 2 - totalWidth / 2 + (currentButton - 1) * (buttonWidth + 4);
    }

    private int createNormalMenuOptions(int topPos, final int spacing) {
        Button singleplayerButton = this.addRenderableWidget(
                Button.builder(Component.translatable("menu.singleplayer"), var1 -> this.minecraft.gui.setScreen(new SelectWorldScreen(this)))
                        .bounds(this.width / 2 - 100, topPos, 200, 20)
                        .build()
        );
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            this.addRenderableWidget(
                    Button.builder(Component.literal("TW"), var1 -> CreateWorldScreen.testWorld(this.minecraft, () -> this.minecraft.gui.setScreen(this)))
                            .bounds(singleplayerButton.getX() + singleplayerButton.getWidth() + 2, topPos, 20, 20)
                            .build()
            );
        }

        Component multiplayerDisabledReason = this.getMultiplayerDisabledReason();
        boolean multiplayerAllowed = multiplayerDisabledReason == null;
        Tooltip tooltip = multiplayerDisabledReason != null ? Tooltip.create(multiplayerDisabledReason) : null;
        int var7;
        this.addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), button -> {
            Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this);
            this.minecraft.gui.setScreen(screen);
        }).bounds(this.width / 2 - 100, var7 = topPos + spacing, 200, 20).tooltip(tooltip).build()).active = multiplayerAllowed;
        this.addRenderableWidget(
                Button.builder(Component.translatable("menu.online"), var1 -> this.minecraft.gui.setScreen(new RealmsMainScreen(this)))
                        .bounds(this.width / 2 - 100, topPos = var7 + spacing, 200, 20)
                        .tooltip(tooltip)
                        .build()
        )
                .active = multiplayerAllowed;
        return topPos;
    }

    private @Nullable Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        } else if (this.minecraft.isNameBanned()) {
            return Component.translatable("title.multiplayer.disabled.banned.name");
        } else {
            BanDetails multiplayerBan = this.minecraft.multiplayerBan();
            if (multiplayerBan != null) {
                return multiplayerBan.expires() != null
                        ? Component.translatable("title.multiplayer.disabled.banned.temporary")
                        : Component.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Component.translatable("title.multiplayer.disabled");
            }
        }
    }

    private int createDemoMenuOptions(int topPos, final int spacing) {
        boolean demoWorldPresent = this.checkDemoWorldPresence();
        this.addRenderableWidget(
                Button.builder(
                        Component.translatable("menu.playdemo"),
                        button -> {
                            if (demoWorldPresent) {
                                this.minecraft.createWorldOpenFlows().openWorld("Demo_World", () -> this.minecraft.gui.setScreen(this));
                            } else {
                                this.minecraft
                                        .createWorldOpenFlows()
                                        .createFreshLevel(
                                                "Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets
                                                        ::createNormalWorldDimensions, this
                                        );
                            }
                        }
                )
                        .bounds(this.width / 2 - 100, topPos, 200, 20)
                        .build()
        );
        int var5;
        Button resetDemoButton = this.addRenderableWidget(
                Button.builder(
                        Component.translatable("menu.resetdemo"),
                        button -> {
                            LevelStorageSource levelSource = this.minecraft.getLevelSource();

                            try (LevelStorageSource.LevelStorageAccess levelAccess = levelSource.createAccess("Demo_World")) {
                                if (levelAccess.hasWorldData()) {
                                    this.minecraft
                                            .gui
                                            .setScreen(
                                            new ConfirmScreen(
                                            this::confirmDemo,
                                            Component.translatable("selectWorld.deleteQuestion"),
                                            Component.translatable("selectWorld.deleteWarning", MinecraftServer.DEMO_SETTINGS.levelName()),
                                            Component.translatable("selectWorld.deleteButton"),
                                            CommonComponents.GUI_CANCEL
                                            )
                                    );
                                }
                            } catch (IOException e) {
                                SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
                                LOGGER.warn("Failed to access demo world", e);
                            }
                        }
                )
                        .bounds(this.width / 2 - 100, var5 = topPos + spacing, 200, 20)
                        .build()
        );
        resetDemoButton.active = demoWorldPresent;
        return var5;
    }

    private boolean checkDemoWorldPresence() {
        try (LevelStorageSource.LevelStorageAccess levelSource = this.minecraft.getLevelSource().createAccess("Demo_World")) {
            return levelSource.hasWorldData();
        } catch (IOException e) {
            SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to read demo world data", e);
            return false;
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }

        float widgetFade = 1.0F;
        if (this.fading) {
            float fade = (float) (Util.getMillis() - this.fadeInStart) / 2000.0F;
            if (fade > 1.0F) {
                this.fading = false;
            } else {
                fade = Mth.clamp(fade, 0.0F, 1.0F);
                widgetFade = Mth.clampedMap(fade, 0.5F, 1.0F, 0.0F, 1.0F);
            }

            this.fadeWidgets(widgetFade);
        }

        this.extractPanorama(graphics, a);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.logoRenderer.extractRenderState(graphics, this.width, this.logoRenderer.keepLogoThroughFade() ? 1.0F : widgetFade);
        if (this.splash != null && !this.minecraft.options.hideSplashTexts().get()) {
            this.splash.extractRenderState(graphics, this.width, this.font, widgetFade);
        }

        // ==================== 原版版本号（上移一行） ====================
        String versionString = "Minecraft " + SharedConstants.getCurrentVersion().name();
        if (this.minecraft.isDemo()) {
            versionString = versionString + " Demo";
        }
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            versionString = versionString + I18n.get("menu.modded");
        }

        // 版本号上移一个字体高度
        graphics.text(
                this.font,
                versionString,
                2,
                this.height - 10 - this.font.lineHeight, // ← 上移一行
                ARGB.white(widgetFade)
        );

        // ==================== 构建时间（显示在原位置） ====================
        Date buildTime = ModMetadata.getBuildTime();
        String buildTimeString = "Build Time: Unknown";
        if (buildTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai")); // 或者用 "GMT+8"
            buildTimeString = "Build Time: " + sdf.format(buildTime);
        }
        graphics.text(
                this.font,
                buildTimeString,
                2,
                this.height - 10,
                ARGB.colorFromFloat(widgetFade, 1.0f, 1.0f, 0.333f) // ← 使用 colorFromFloat
        );
        if (this.realmsNotificationsEnabled() && widgetFade >= 1.0F) {
            this.realmsNotificationsScreen.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {}

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return super.mouseClicked(event, doubleClick)
                ? true
                : this.realmsNotificationsEnabled() && this.realmsNotificationsScreen.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.removed();
        }
    }

    @Override
    public void added() {
        super.added();
        if (this.realmsNotificationsScreen != null) {
            this.realmsNotificationsScreen.added();
        }
    }

    private void confirmDemo(final boolean result) {
        if (result) {
            try (LevelStorageSource.LevelStorageAccess levelSource = this.minecraft.getLevelSource().createAccess("Demo_World")) {
                levelSource.deleteLevel();
            } catch (IOException e) {
                SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
                LOGGER.warn("Failed to delete demo world", e);
            }
        }

        this.minecraft.gui.setScreen(this);
    }

    @Override
    public boolean canInterruptWithAnotherScreen() {
        return true;
    }
}