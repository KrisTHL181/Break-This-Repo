package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private final RealmsMainScreen lastScreen;
    private @Nullable RealmsServer serverData;
    private @Nullable PreferredRegionsDto regions;
    private @Nullable Subscription subscription;
    private final Map<RealmsRegion, ServiceQuality> regionServiceQuality = new LinkedHashMap<>();
    private final long serverId;
    private boolean stateChanged;
    private final TabManager tabManager = new TabManager(
        x$0 -> this.addRenderableWidget(x$0), x$0 -> this.removeWidget(x$0), this::onTabSelected, this::onTabDeselected
    );
    private @Nullable Button playButton;
    private @Nullable MenuTabBar tabNavigationBar;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public RealmsConfigureWorldScreen(
        final RealmsMainScreen lastScreen, final long serverId, final @Nullable RealmsServer serverData, final @Nullable PreferredRegionsDto regions
    ) {
        super(Component.empty());
        this.lastScreen = lastScreen;
        this.serverId = serverId;
        this.serverData = serverData;
        this.regions = regions;
    }

    public RealmsConfigureWorldScreen(final RealmsMainScreen lastScreen, final long serverId) {
        this(lastScreen, serverId, null, null);
    }

    @Override
    public void init() {
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        }

        if (this.regions == null) {
            this.fetchRegionData();
        }

        if (this.subscription == null) {
            this.fetchSubscription(this.serverId);
        }

        Component loadingTitle = Component.translatable("mco.configure.world.loading");
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width)
            .addTabs(
                new LoadingTab(this.getFont(), RealmsWorldsTab.TITLE, loadingTitle),
                new LoadingTab(this.getFont(), RealmsPlayersTab.TITLE, loadingTitle),
                new LoadingTab(this.getFont(), RealmsSubscriptionTab.TITLE, loadingTitle),
                new LoadingTab(this.getFont(), RealmsSettingsTab.TITLE, loadingTitle)
            )
            .build();
        this.tabNavigationBar.setTabActiveState(3, false);
        this.addRenderableWidget(this.tabNavigationBar);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.playButton = footer.addChild(Button.builder(PLAY_TEXT, button -> {
            this.onClose();
            RealmsMainScreen.play(this.serverData, this);
        }).width(150).build());
        this.playButton.active = false;
        footer.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(button -> {
            button.setTabOrderGroup(1);
            this.addRenderableWidget(button);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        if (this.serverData != null && this.regions != null && this.subscription != null) {
            this.onRealmsDataFetched();
        }
    }

    private void onTabSelected(final Tab tab) {
        if (this.serverData != null && tab instanceof RealmsConfigurationTab configurationTab) {
            configurationTab.onSelected(this.serverData);
        }
    }

    private void onTabDeselected(final Tab tab) {
        if (this.serverData != null && tab instanceof RealmsConfigurationTab configurationTab) {
            configurationTab.onDeselected(this.serverData);
        }
    }

    public int getContentHeight() {
        return this.layout.getContentHeight();
    }

    public int getHeaderHeight() {
        return this.layout.getHeaderHeight();
    }

    public Screen getLastScreen() {
        return this.lastScreen;
    }

    public Screen createErrorScreen(final RealmsServiceException exception) {
        return new RealmsGenericErrorScreen(exception, this.lastScreen);
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.arrangeElements(this.width);
            int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
            this.tabManager.setTabArea(tabArea);
            this.layout.setHeaderHeight(tabAreaTop);
            this.layout.arrangeElements();
        }
    }

    private void updateButtonStates() {
        if (this.serverData != null && this.playButton != null) {
            this.playButton.active = this.serverData.shouldPlayButtonBeActive();
            if (!this.playButton.active && this.serverData.state == RealmsServer.State.CLOSED) {
                this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
            }
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2
        );
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.tabNavigationBar.keyPressed(event) ? true : super.keyPressed(event);
    }

    @Override
    protected void extractMenuBackground(final GuiGraphicsExtractor graphics) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16
        );
        this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        if (this.serverData != null && this.tabManager.getCurrentTab() instanceof RealmsConfigurationTab tab) {
            tab.onDeselected(this.serverData);
        }

        this.minecraft.gui.setScreen(this.lastScreen);
        if (this.stateChanged) {
            this.lastScreen.resetScreen();
        }
    }

    public void fetchRegionData() {
        RealmsUtil.supplyAsync(
                RealmsClient::getPreferredRegionSelections, RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get realms region data")
            )
            .thenAcceptAsync(regions -> {
                this.regions = regions;
                this.onRealmsDataFetched();
            }, this.minecraft);
    }

    public void fetchServerData(final long realmId) {
        RealmsUtil.<RealmsServer>supplyAsync(
                client -> client.getOwnRealm(realmId), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world")
            )
            .thenAcceptAsync(serverData -> {
                this.serverData = serverData;
                this.onRealmsDataFetched();
            }, this.minecraft);
    }

    public void fetchSubscription(final long realmId) {
        RealmsUtil.<Subscription>supplyAsync(client -> client.subscriptionFor(realmId), e -> {
            LOGGER.error("Couldn't get subscription", e);
            this.minecraft.execute(() -> {
                if (this.minecraft.gui.screen() instanceof RealmsConfigureWorldScreen) {
                    this.minecraft.gui.setScreen(this.createErrorScreen(e));
                }
            });
        }).thenAcceptAsync(subscription -> {
            this.subscription = subscription;
            this.onRealmsDataFetched();
        }, this.minecraft);
    }

    private void onRealmsDataFetched() {
        if (this.serverData != null && this.regions != null && this.subscription != null) {
            this.regionServiceQuality.clear();

            for (RegionDataDto region : this.regions.regionData()) {
                if (region.region() != RealmsRegion.INVALID_REGION) {
                    this.regionServiceQuality.put(region.region(), region.serviceQuality());
                }
            }

            int focusedTabIndex = -1;
            if (this.tabNavigationBar != null) {
                focusedTabIndex = this.tabNavigationBar.getTabs().indexOf(this.tabManager.getCurrentTab());
            }

            if (this.tabNavigationBar != null) {
                this.removeWidget(this.tabNavigationBar);
            }

            this.tabNavigationBar = this.addRenderableWidget(
                MenuTabBar.builder(this.tabManager, this.width)
                    .addTabs(
                        new RealmsWorldsTab(this, Objects.requireNonNull(this.minecraft), this.serverData),
                        new RealmsPlayersTab(this, this.minecraft, this.serverData),
                        new RealmsSubscriptionTab(this, this.minecraft, this.serverData, this.subscription),
                        new RealmsSettingsTab(this, this.minecraft, this.serverData, this.regionServiceQuality)
                    )
                    .build()
            );
            this.setFocused(this.tabNavigationBar);
            if (focusedTabIndex != -1) {
                this.tabNavigationBar.selectTab(focusedTabIndex, false);
            }

            this.tabNavigationBar.setTabActiveState(3, !this.serverData.expired);
            if (this.serverData.expired) {
                this.tabNavigationBar.setTabTooltip(3, Tooltip.create(Component.translatable("mco.configure.world.settings.expired")));
            } else {
                this.tabNavigationBar.setTabTooltip(3, null);
            }

            this.updateButtonStates();
            this.repositionElements();
        }
    }

    public void saveSlotSettings(final RealmsSlot slot) {
        RealmsSlot oldSlot = this.serverData.slots.get(this.serverData.activeSlot);
        slot.options.templateId = oldSlot.options.templateId;
        slot.options.templateImage = oldSlot.options.templateImage;
        RealmsClient client = RealmsClient.getOrCreate();

        try {
            if (this.serverData.activeSlot != slot.slotId) {
                throw new RealmsServiceException(RealmsError.CustomError.configurationError());
            }

            client.updateSlot(this.serverData.id, slot.slotId, slot.options, slot.settings);
            this.serverData.slots.put(this.serverData.activeSlot, slot);
            if (slot.options.gameMode != oldSlot.options.gameMode || slot.isHardcore() != oldSlot.isHardcore()) {
                RealmsMainScreen.refreshServerList();
            }

            this.stateChanged();
        } catch (RealmsServiceException e) {
            LOGGER.error("Couldn't save slot settings", e);
            this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(e, this));
            return;
        }

        this.minecraft.gui.setScreen(this);
    }

    public void saveSettings(final String name, final String desc, final RegionSelectionPreference preference, final @Nullable RealmsRegion region) {
        String description = StringUtil.isBlank(desc) ? "" : desc;
        String finalName = StringUtil.isBlank(name) ? "" : name;
        RealmsClient client = RealmsClient.getOrCreate();

        try {
            RealmsSlot realmsSlot = this.serverData.slots.get(this.serverData.activeSlot);
            RealmsRegion regionSelection = preference == RegionSelectionPreference.MANUAL ? region : null;
            RegionSelectionPreferenceDto regionSelectionPreference = new RegionSelectionPreferenceDto(preference, regionSelection);
            client.updateConfiguration(
                this.serverData.id, finalName, description, regionSelectionPreference, realmsSlot.slotId, realmsSlot.options, realmsSlot.settings
            );
            this.serverData.regionSelectionPreference = regionSelectionPreference;
            this.serverData.name = name;
            this.serverData.motd = description;
            this.stateChanged();
        } catch (RealmsServiceException e) {
            LOGGER.error("Couldn't save settings", e);
            this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(e, this));
            return;
        }

        this.minecraft.gui.setScreen(this);
    }

    public void openTheWorld(final boolean join) {
        RealmsConfigureWorldScreen screenWithKnownData = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft
            .gui
            .setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new OpenServerTask(this.serverData, screenWithKnownData, join, this.minecraft)));
    }

    public void closeTheWorld() {
        RealmsConfigureWorldScreen screenWithKnownData = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new CloseServerTask(this.serverData, screenWithKnownData)));
    }

    public void stateChanged() {
        this.stateChanged = true;
        if (this.tabNavigationBar != null) {
            for (Tab child : this.tabNavigationBar.getTabs()) {
                if (child instanceof RealmsConfigurationTab tab) {
                    tab.updateData(this.serverData);
                }
            }
        }
    }

    public boolean invitePlayer(final long serverId, final String name) {
        RealmsClient client = RealmsClient.getOrCreate();

        try {
            List<PlayerInfo> players = client.invite(serverId, name);
            if (this.serverData != null) {
                this.serverData.players = players;
            } else {
                this.serverData = client.getOwnRealm(serverId);
            }

            this.stateChanged();
            return true;
        } catch (RealmsServiceException e) {
            LOGGER.error("Couldn't invite user", e);
            return false;
        }
    }

    public RealmsConfigureWorldScreen getNewScreen() {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
        realmsConfigureWorldScreen.stateChanged = this.stateChanged;
        return realmsConfigureWorldScreen;
    }

    public RealmsConfigureWorldScreen getNewScreenWithKnownData(final RealmsServer serverData) {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId, serverData, this.regions);
        realmsConfigureWorldScreen.stateChanged = this.stateChanged;
        return realmsConfigureWorldScreen;
    }
}