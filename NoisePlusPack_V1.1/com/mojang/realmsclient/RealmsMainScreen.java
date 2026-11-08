package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final Identifier INFO_SPRITE = Identifier.withDefaultNamespace("icon/info");
   static final Identifier NEW_REALM_SPRITE = Identifier.withDefaultNamespace("icon/new_realm");
   static final Identifier EXPIRED_SPRITE = Identifier.withDefaultNamespace("realm_status/expired");
   static final Identifier EXPIRES_SOON_SPRITE = Identifier.withDefaultNamespace("realm_status/expires_soon");
   static final Identifier OPEN_SPRITE = Identifier.withDefaultNamespace("realm_status/open");
   static final Identifier CLOSED_SPRITE = Identifier.withDefaultNamespace("realm_status/closed");
   private static final Identifier INVITE_SPRITE = Identifier.withDefaultNamespace("icon/invite");
   private static final Identifier NEWS_SPRITE = Identifier.withDefaultNamespace("icon/news");
   public static final Identifier HARDCORE_MODE_SPRITE = Identifier.withDefaultNamespace("hud/heart/hardcore_full");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier NO_REALMS_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/no_realms.png");
   private static final Component TITLE = Component.translatable("menu.online");
   private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
   static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
   private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
   private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
   private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
   private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_COLUMNS = 3;
   private static final int BUTTON_SPACING = 4;
   private static final int CONTENT_WIDTH = 308;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static final int FOOTER_PADDING = 11;
   private static final int NEW_REALM_SPRITE_WIDTH = 40;
   private static final int NEW_REALM_SPRITE_HEIGHT = 20;
   private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().stable();
   private static boolean snapshotToggle = SNAPSHOT;
   private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
   private DataFetcher.@Nullable Subscription dataSubscription;
   private final Set<UUID> handledSeenNotifications = new HashSet<>();
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private final Screen lastScreen;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   RealmsMainScreen.RealmSelectionList realmSelectionList;
   RealmsServerList serverList;
   List<RealmsServer> availableSnapshotServers = List.of();
   RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists(Map.of());
   private volatile boolean trialsAvailable;
   private volatile @Nullable String newsLink;
   final List<RealmsNotification> notifications = new ArrayList<>();
   private Button addRealmButton;
   private RealmsMainScreen.NotificationButton pendingInvitesButton;
   private RealmsMainScreen.NotificationButton newsButton;
   private RealmsMainScreen.LayoutState activeLayoutState;
   private @Nullable HeaderAndFooterLayout layout;

   public RealmsMainScreen(Screen p_86315_) {
      super(TITLE);
      this.lastScreen = p_86315_;
      this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
   }

   @Override
   public void init() {
      this.serverList = new RealmsServerList(this.minecraft);
      this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
      Component component = Component.translatable("mco.invites.title");
      this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
         component, INVITE_SPRITE, p_296029_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component)), null
      );
      Component component1 = Component.translatable("mco.news");
      this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, p_296035_ -> {
         String s = this.newsLink;
         if (s != null) {
            ConfirmLinkScreen.confirmLinkNow(this, s);
            if (this.newsButton.notificationCount() != 0) {
               RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
               realmspersistence$realmspersistencedata.hasUnreadNews = false;
               RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
               this.newsButton.setNotificationCount(0);
            }
         }
      }, component1);
      this.playButton = Button.builder(PLAY_TEXT, p_86659_ -> play(this.getSelectedServer(), this)).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
      this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_296032_ -> this.openTrialAvailablePopup())
         .size(100, 20)
         .build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, p_325094_ -> this.onClose()).width(100).build();
      this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"), snapshotToggle)
            .create(5, 5, 100, 20, Component.literal("Realm"), (p_308035_, p_308036_) -> {
               snapshotToggle = p_308036_;
               this.availableSnapshotServers = List.of();
               this.debugRefreshDataFetchers();
            })
      );
      this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
      this.updateButtonStates();
      this.availability.thenAcceptAsync(p_296034_ -> {
         Screen screen = p_296034_.createErrorScreen(this.lastScreen);
         if (screen == null) {
            this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.minecraft.setScreen(screen);
         }
      }, this.screenExecutor);
   }

   public static boolean isSnapshot() {
      return SNAPSHOT && snapshotToggle;
   }

   @Override
   protected void repositionElements() {
      if (this.layout != null) {
         this.realmSelectionList.updateSize(this.width, this.layout);
         this.layout.arrangeElements();
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void updateLayout() {
      if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
         this.updateLayout(RealmsMainScreen.LayoutState.NO_REALMS);
      } else {
         this.updateLayout(RealmsMainScreen.LayoutState.LIST);
      }
   }

   private void updateLayout(RealmsMainScreen.LayoutState p_297284_) {
      if (this.activeLayoutState != p_297284_) {
         if (this.layout != null) {
            this.layout.visitWidgets(p_325098_ -> this.removeWidget(p_325098_));
         }

         this.layout = this.createLayout(p_297284_);
         this.activeLayoutState = p_297284_;
         this.layout.visitWidgets(p_325096_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_325096_);
         });
         this.repositionElements();
      }
   }

   private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState p_299759_) {
      HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
      headerandfooterlayout.setHeaderHeight(44);
      headerandfooterlayout.addToHeader(this.createHeader());
      Layout layout = this.createFooter(p_299759_);
      layout.arrangeElements();
      headerandfooterlayout.setFooterHeight(layout.getHeight() + 22);
      headerandfooterlayout.addToFooter(layout);
      switch (p_299759_) {
         case LOADING:
            headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
            break;
         case NO_REALMS:
            headerandfooterlayout.addToContents(this.createNoRealmsContent());
            break;
         case LIST:
            headerandfooterlayout.addToContents(this.realmSelectionList);
      }

      return headerandfooterlayout;
   }

   private Layout createHeader() {
      int i = 90;
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
      linearlayout.defaultCellSetting().alignVerticallyMiddle();
      linearlayout.addChild(this.pendingInvitesButton);
      linearlayout.addChild(this.newsButton);
      LinearLayout linearlayout1 = LinearLayout.horizontal();
      linearlayout1.defaultCellSetting().alignVerticallyMiddle();
      linearlayout1.addChild(SpacerElement.width(90));
      linearlayout1.addChild(realmsLogo(), LayoutSettings::alignHorizontallyCenter);
      linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
      return linearlayout1;
   }

   private Layout createFooter(RealmsMainScreen.LayoutState p_299205_) {
      GridLayout gridlayout = new GridLayout().spacing(4);
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
      if (p_299205_ == RealmsMainScreen.LayoutState.LIST) {
         gridlayout$rowhelper.addChild(this.playButton);
         gridlayout$rowhelper.addChild(this.configureButton);
         gridlayout$rowhelper.addChild(this.renewButton);
         gridlayout$rowhelper.addChild(this.leaveButton);
      }

      gridlayout$rowhelper.addChild(this.addRealmButton);
      gridlayout$rowhelper.addChild(this.backButton);
      return gridlayout;
   }

   private LinearLayout createNoRealmsContent() {
      LinearLayout linearlayout = LinearLayout.vertical().spacing(8);
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
      linearlayout.addChild(
         FocusableTextWidget.builder(NO_REALMS_TEXT, this.font)
            .maxWidth(308)
            .alwaysShowBorder(false)
            .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
            .build()
      );
      return linearlayout;
   }

   void updateButtonStates() {
      RealmsServer realmsserver = this.getSelectedServer();
      boolean flag = realmsserver != null;
      this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
      this.playButton.active = flag && realmsserver.shouldPlayButtonBeActive();
      if (!this.playButton.active && flag && realmsserver.state == RealmsServer.State.CLOSED) {
         this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
      }

      this.renewButton.active = flag && this.shouldRenewButtonBeActive(realmsserver);
      this.leaveButton.active = flag && this.shouldLeaveButtonBeActive(realmsserver);
      this.configureButton.active = flag && this.shouldConfigureButtonBeActive(realmsserver);
   }

   private boolean shouldRenewButtonBeActive(RealmsServer p_86595_) {
      return p_86595_.expired && isSelfOwnedServer(p_86595_);
   }

   private boolean shouldConfigureButtonBeActive(RealmsServer p_86620_) {
      return isSelfOwnedServer(p_86620_) && p_86620_.state != RealmsServer.State.UNINITIALIZED;
   }

   private boolean shouldLeaveButtonBeActive(RealmsServer p_86645_) {
      return !isSelfOwnedServer(p_86645_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }
   }

   public static void refreshPendingInvites() {
      Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
   }

   public static void refreshServerList() {
      Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
   }

   private void debugRefreshDataFetchers() {
      for (DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
         task.reset();
      }
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_238836_) {
      DataFetcher.Subscription datafetcher$subscription = p_238836_.dataFetcher.createSubscription();
      datafetcher$subscription.subscribe(p_238836_.serverListUpdateTask, p_308037_ -> {
         this.serverList.updateServersList(p_308037_.serverList());
         this.availableSnapshotServers = p_308037_.availableSnapshotServers();
         this.refreshListAndLayout();
         boolean flag = false;

         for (RealmsServer realmsserver : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
               flag = true;
            }
         }

         if (!regionsPinged && flag) {
            regionsPinged = true;
            this.pingRegions();
         }
      });
      callRealmsClient(RealmsClient::getNotifications, p_274622_ -> {
         this.notifications.clear();
         this.notifications.addAll(p_274622_);

         for (RealmsNotification realmsnotification : p_274622_) {
            if (realmsnotification instanceof RealmsNotification.InfoPopup realmsnotification$infopopup) {
               PopupScreen popupscreen = realmsnotification$infopopup.buildScreen(this, this::dismissNotification);
               if (popupscreen != null) {
                  this.minecraft.setScreen(popupscreen);
                  this.markNotificationsAsSeen(List.of(realmsnotification));
                  break;
               }
            }
         }

         if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
            this.refreshListAndLayout();
         }
      });
      datafetcher$subscription.subscribe(p_238836_.pendingInvitesTask, p_296027_ -> {
         this.pendingInvitesButton.setNotificationCount(p_296027_);
         this.pendingInvitesButton.setTooltip(p_296027_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
         if (p_296027_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().saySystemNow(Component.translatable("mco.configure.world.invite.narration", p_296027_));
         }
      });
      datafetcher$subscription.subscribe(p_238836_.trialAvailabilityTask, p_296031_ -> this.trialsAvailable = p_296031_);
      datafetcher$subscription.subscribe(p_238836_.onlinePlayersTask, p_340705_ -> this.onlinePlayersPerRealm = p_340705_);
      datafetcher$subscription.subscribe(p_238836_.newsTask, p_296037_ -> {
         p_238836_.newsManager.updateUnreadNews(p_296037_);
         this.newsLink = p_238836_.newsManager.newsLink();
         this.newsButton.setNotificationCount(p_238836_.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
      });
      return datafetcher$subscription;
   }

   void markNotificationsAsSeen(Collection<RealmsNotification> p_311351_) {
      List<UUID> list = new ArrayList<>(p_311351_.size());

      for (RealmsNotification realmsnotification : p_311351_) {
         if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
            list.add(realmsnotification.uuid());
         }
      }

      if (!list.isEmpty()) {
         callRealmsClient(p_274625_ -> {
            p_274625_.notificationsSeen(list);
            return null;
         }, p_274630_ -> this.handledSeenNotifications.addAll(list));
      }
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> p_275561_, Consumer<T> p_275686_) {
      Minecraft minecraft = Minecraft.getInstance();
      CompletableFuture.<T>supplyAsync(() -> {
         try {
            return p_275561_.request(RealmsClient.getOrCreate(minecraft));
         } catch (RealmsServiceException realmsserviceexception) {
            throw new RuntimeException(realmsserviceexception);
         }
      }).thenAcceptAsync(p_275686_, minecraft).exceptionally(p_274626_ -> {
         LOGGER.error("Failed to execute call to Realms Service", p_274626_);
         return null;
      });
   }

   private void refreshListAndLayout() {
      this.realmSelectionList.refreshEntries(this);
      this.updateLayout();
      this.updateButtonStates();
   }

   private void pingRegions() {
      new Thread(() -> {
         List<RegionPingResult> list = Ping.pingAllRegions();
         RealmsClient realmsclient = RealmsClient.getOrCreate();
         PingResult pingresult = new PingResult(list, this.getOwnedNonExpiredRealmIds());

         try {
            realmsclient.sendPingResults(pingresult);
         } catch (Throwable throwable) {
            LOGGER.warn("Could not send ping result to Realms: ", throwable);
         }
      }).start();
   }

   private List<Long> getOwnedNonExpiredRealmIds() {
      List<Long> list = Lists.newArrayList();

      for (RealmsServer realmsserver : this.serverList) {
         if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
            list.add(realmsserver.id);
         }
      }

      return list;
   }

   private void onRenew(@Nullable RealmsServer p_193500_) {
      if (p_193500_ != null) {
         String s = CommonLinks.extendRealms(p_193500_.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), p_193500_.expiredTrial);
         this.minecraft.setScreen(new ConfirmLinkScreen(p_447729_ -> {
            if (p_447729_) {
               Util.getPlatform().openUri(s);
            } else {
               this.minecraft.setScreen(this);
            }
         }, s, true));
      }
   }

   private void configureClicked(@Nullable RealmsServer p_86657_) {
      if (p_86657_ != null && this.minecraft.isLocalPlayer(p_86657_.ownerUUID)) {
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, p_86657_.id));
      }
   }

   private void leaveClicked(@Nullable RealmsServer p_86670_) {
      if (p_86670_ != null && !this.minecraft.isLocalPlayer(p_86670_.ownerUUID)) {
         Component component = Component.translatable("mco.configure.world.leave.question.line1");
         this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, component, p_340701_ -> this.leaveServer(p_86670_)));
      }
   }

   private @Nullable RealmsServer getSelectedServer() {
      return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
         ? realmsmainscreen$serverentry.getServer()
         : null;
   }

   private void leaveServer(final RealmsServer p_193495_) {
      (new Thread("Realms-leave-server") {
            @Override
            public void run() {
               try {
                  RealmsClient realmsclient = RealmsClient.getOrCreate();
                  realmsclient.uninviteMyselfFrom(p_193495_.id);
                  RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsMainScreen.LOGGER.error("Couldn't configure world", realmsserviceexception);
                  RealmsMainScreen.this.minecraft
                     .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this)));
               }
            }
         })
         .start();
      this.minecraft.setScreen(this);
   }

   void dismissNotification(UUID p_275349_) {
      callRealmsClient(p_274628_ -> {
         p_274628_.notificationsDismiss(List.of(p_275349_));
         return null;
      }, p_274632_ -> {
         this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && p_275349_.equals(p_274621_.uuid()));
         this.refreshListAndLayout();
      });
   }

   public void resetScreen() {
      this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      refreshServerList();
   }

   @Override
   public Component getNarrationMessage() {
      return switch (this.activeLayoutState) {
         case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
         case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
         case LIST -> super.getNarrationMessage();
      };
   }

   @Override
   public void render(GuiGraphics p_282736_, int p_283347_, int p_282480_, float p_283485_) {
      super.render(p_282736_, p_283347_, p_282480_, p_283485_);
      if (isSnapshot()) {
         p_282736_.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().name(), 2, this.height - 10, -1);
      }

      if (this.trialsAvailable && this.addRealmButton.active) {
         AddRealmPopupScreen.renderDiamond(p_282736_, this.addRealmButton);
      }

      switch (RealmsClient.ENVIRONMENT) {
         case STAGE:
            this.renderEnvironment(p_282736_, "STAGE!", -256);
            break;
         case LOCAL:
            this.renderEnvironment(p_282736_, "LOCAL!", -8388737);
      }
   }

   private void openTrialAvailablePopup() {
      this.minecraft.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
   }

   public static void play(@Nullable RealmsServer p_86516_, Screen p_86517_) {
      play(p_86516_, p_86517_, false);
   }

   public static void play(@Nullable RealmsServer p_312669_, Screen p_310591_, boolean p_309776_) {
      if (p_312669_ != null) {
         if (!isSnapshot() || p_309776_ || p_312669_.isMinigameActive()) {
            Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_310591_, new GetServerDetailsTask(p_310591_, p_312669_)));
            return;
         }

         switch (p_312669_.compatibility) {
            case COMPATIBLE:
               Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_310591_, new GetServerDetailsTask(p_310591_, p_312669_)));
               break;
            case UNVERIFIABLE:
               confirmToPlay(
                  p_312669_,
                  p_310591_,
                  Component.translatable("mco.compatibility.unverifiable.title").withColor(-171),
                  Component.translatable("mco.compatibility.unverifiable.message"),
                  CommonComponents.GUI_CONTINUE
               );
               break;
            case NEEDS_DOWNGRADE:
               confirmToPlay(
                  p_312669_,
                  p_310591_,
                  Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
                  Component.translatable(
                     "mco.compatibility.downgrade.description",
                     Component.literal(p_312669_.activeVersion).withColor(-171),
                     Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)
                  ),
                  Component.translatable("mco.compatibility.downgrade")
               );
               break;
            case NEEDS_UPGRADE:
               upgradeRealmAndPlay(p_312669_, p_310591_);
               break;
            case INCOMPATIBLE:
               Minecraft.getInstance()
                  .setScreen(
                     new PopupScreen.Builder(p_310591_, INCOMPATIBLE_POPUP_TITLE)
                        .setMessage(
                           Component.translatable(
                              "mco.compatibility.incompatible.series.popup.message",
                              Component.literal(p_312669_.activeVersion).withColor(-171),
                              Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)
                           )
                        )
                        .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                        .build()
                  );
               break;
            case RELEASE_TYPE_INCOMPATIBLE:
               Minecraft.getInstance()
                  .setScreen(
                     new PopupScreen.Builder(p_310591_, INCOMPATIBLE_POPUP_TITLE)
                        .setMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE)
                        .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                        .build()
                  );
         }
      }
   }

   private static void confirmToPlay(RealmsServer p_311893_, Screen p_310296_, Component p_309987_, Component p_309434_, Component p_311253_) {
      Minecraft.getInstance().setScreen(new PopupScreen.Builder(p_310296_, p_309987_).setMessage(p_309434_).addButton(p_311253_, p_340704_ -> {
         Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_310296_, new GetServerDetailsTask(p_310296_, p_311893_)));
         refreshServerList();
      }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
   }

   private static void upgradeRealmAndPlay(RealmsServer p_343214_, Screen p_343698_) {
      Component component = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
      Component component1 = Component.translatable("mco.compatibility.upgrade");
      Component component2 = Component.literal(p_343214_.activeVersion).withColor(-171);
      Component component3 = Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171);
      Component component4 = isSelfOwnedServer(p_343214_)
         ? Component.translatable("mco.compatibility.upgrade.description", component2, component3)
         : Component.translatable("mco.compatibility.upgrade.friend.description", component2, component3);
      confirmToPlay(p_343214_, p_343698_, component, component4, component1);
   }

   public static Component getVersionComponent(String p_312049_, boolean p_312280_) {
      return getVersionComponent(p_312049_, p_312280_ ? -8355712 : -2142128);
   }

   public static Component getVersionComponent(String p_311695_, int p_311083_) {
      return StringUtils.isBlank(p_311695_) ? CommonComponents.EMPTY : Component.literal(p_311695_).withColor(p_311083_);
   }

   public static Component getGameModeComponent(int p_364136_, boolean p_361479_) {
      return p_361479_ ? Component.translatable("gameMode.hardcore").withColor(-65536) : GameType.byId(p_364136_).getLongDisplayName();
   }

   static boolean isSelfOwnedServer(RealmsServer p_86684_) {
      return Minecraft.getInstance().isLocalPlayer(p_86684_.ownerUUID);
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer p_86689_) {
      return isSelfOwnedServer(p_86689_) && !p_86689_.expired;
   }

   private void renderEnvironment(GuiGraphics p_298843_, String p_299597_, int p_300122_) {
      p_298843_.pose().pushMatrix();
      p_298843_.pose().translate(this.width / 2 - 25, 20.0F);
      p_298843_.pose().rotate((float) (-Math.PI / 9));
      p_298843_.pose().scale(1.5F, 1.5F);
      p_298843_.drawString(this.font, p_299597_, 0, 0, p_300122_);
      p_298843_.pose().popMatrix();
   }

   @OnlyIn(Dist.CLIENT)
   class AvailableSnapshotEntry extends RealmsMainScreen.Entry {
      private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
      private static final int TEXT_PADDING = 5;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
      private final RealmsServer parent;

      public AvailableSnapshotEntry(final RealmsServer p_311559_) {
         this.parent = p_311559_;
         this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
      }

      @Override
      public void renderContent(GuiGraphics p_429265_, int p_426747_, int p_423981_, boolean p_427466_, float p_429302_) {
         p_429265_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
         int i = this.getContentYMiddle() - 4;
         p_429265_.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, this.getContentX() + 40 - 2, i - 5, -8388737);
         p_429265_.drawString(
            RealmsMainScreen.this.font,
            Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server")),
            this.getContentX() + 40 - 2,
            i + 5,
            -8355712
         );
         this.tooltip
            .refreshTooltipForNextRenderPass(
               p_429265_,
               p_426747_,
               p_423981_,
               p_427466_,
               this.isFocused(),
               new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
            );
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_426951_, boolean p_423382_) {
         this.addSnapshotRealm();
         return true;
      }

      @Override
      public boolean keyPressed(KeyEvent p_429423_) {
         if (p_429423_.isSelection()) {
            this.addSnapshotRealm();
            return false;
         } else {
            return super.keyPressed(p_429423_);
         }
      }

      private void addSnapshotRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.this.minecraft
            .setScreen(
               new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title"))
                  .setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text"))
                  .addButton(
                     Component.translatable("mco.selectServer.create"),
                     p_420584_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent, true))
                  )
                  .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                  .build()
            );
      }

      @Override
      public Component getNarration() {
         return Component.translatable(
            "gui.narrate.button",
            CommonComponents.joinForNarration(
               START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server"))
            )
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CrossButton extends ImageButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(
         Identifier.withDefaultNamespace("widget/cross_button"), Identifier.withDefaultNamespace("widget/cross_button_highlighted")
      );

      protected CrossButton(Button.OnPress p_275420_, Component p_275193_) {
         super(0, 0, 14, 14, SPRITES, p_275420_);
         this.setTooltip(Tooltip.create(p_275193_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      protected static final int STATUS_LIGHT_WIDTH = 10;
      private static final int STATUS_LIGHT_HEIGHT = 28;
      protected static final int PADDING_X = 7;
      protected static final int PADDING_Y = 2;

      protected void renderStatusLights(RealmsServer p_312488_, GuiGraphics p_310620_, int p_309999_, int p_309772_, int p_310609_, int p_312927_) {
         int i = p_309999_ - 10 - 7;
         int j = p_309772_ + 2;
         if (p_312488_.expired) {
            this.drawRealmStatus(p_310620_, i, j, p_310609_, p_312927_, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
         } else if (p_312488_.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(p_310620_, i, j, p_310609_, p_312927_, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
         } else if (RealmsMainScreen.isSelfOwnedServer(p_312488_) && p_312488_.daysLeft < 7) {
            this.drawRealmStatus(
               p_310620_,
               i,
               j,
               p_310609_,
               p_312927_,
               RealmsMainScreen.EXPIRES_SOON_SPRITE,
               () -> {
                  if (p_312488_.daysLeft <= 0) {
                     return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                  } else {
                     return p_312488_.daysLeft == 1
                        ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                        : Component.translatable("mco.selectServer.expires.days", p_312488_.daysLeft);
                  }
               }
            );
         } else if (p_312488_.state == RealmsServer.State.OPEN) {
            this.drawRealmStatus(p_310620_, i, j, p_310609_, p_312927_, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
         }
      }

      private void drawRealmStatus(
         GuiGraphics p_312727_, int p_311004_, int p_311259_, int p_310947_, int p_311421_, Identifier p_453549_, Supplier<Component> p_312584_
      ) {
         p_312727_.blitSprite(RenderPipelines.GUI_TEXTURED, p_453549_, p_311004_, p_311259_, 10, 28);
         if (RealmsMainScreen.this.realmSelectionList.isMouseOver(p_310947_, p_311421_)
            && p_310947_ >= p_311004_
            && p_310947_ <= p_311004_ + 10
            && p_311421_ >= p_311259_
            && p_311421_ <= p_311259_ + 28) {
            p_312727_.setTooltipForNextFrame(p_312584_.get(), p_310947_, p_311421_);
         }
      }

      protected void renderFirstLine(GuiGraphics p_410714_, int p_407007_, int p_408894_, int p_408438_, int p_408372_, RealmsServer p_410210_) {
         int i = this.textX(p_408894_);
         int j = this.firstLineY(p_407007_);
         Component component = RealmsMainScreen.getVersionComponent(p_410210_.activeVersion, p_410210_.isCompatible());
         int k = this.versionTextX(p_408894_, p_408438_, component);
         this.renderClampedString(p_410714_, p_410210_.getName(), i, j, k, p_408372_);
         if (component != CommonComponents.EMPTY && !p_410210_.isMinigameActive()) {
            p_410714_.drawString(RealmsMainScreen.this.font, component, k, j, -8355712);
         }
      }

      protected void renderSecondLine(GuiGraphics p_405882_, int p_408513_, int p_408357_, int p_407540_, RealmsServer p_406944_) {
         int i = this.textX(p_408357_);
         int j = this.firstLineY(p_408513_);
         int k = this.secondLineY(j);
         String s = p_406944_.getMinigameName();
         boolean flag = p_406944_.isMinigameActive();
         if (flag && s != null) {
            Component component = Component.literal(s).withStyle(ChatFormatting.GRAY);
            p_405882_.drawString(RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", component).withColor(-171), i, k, -1);
         } else {
            int l = this.renderGameMode(p_406944_, p_405882_, p_408357_, p_407540_, j);
            this.renderClampedString(p_405882_, p_406944_.getDescription(), i, this.secondLineY(j), l, -8355712);
         }
      }

      protected void renderThirdLine(GuiGraphics p_309875_, int p_309431_, int p_312885_, RealmsServer p_311246_) {
         int i = this.textX(p_312885_);
         int j = this.firstLineY(p_309431_);
         int k = this.thirdLineY(j);
         if (!RealmsMainScreen.isSelfOwnedServer(p_311246_)) {
            p_309875_.drawString(RealmsMainScreen.this.font, p_311246_.owner, i, this.thirdLineY(j), -8355712);
         } else if (p_311246_.expired) {
            Component component = p_311246_.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
            p_309875_.drawString(RealmsMainScreen.this.font, component, i, k, -2142128);
         }
      }

      protected void renderClampedString(GuiGraphics p_311967_, @Nullable String p_310470_, int p_311349_, int p_310646_, int p_312217_, int p_310447_) {
         if (p_310470_ != null) {
            int i = p_312217_ - p_311349_;
            if (RealmsMainScreen.this.font.width(p_310470_) > i) {
               String s = RealmsMainScreen.this.font.plainSubstrByWidth(p_310470_, i - RealmsMainScreen.this.font.width("... "));
               p_311967_.drawString(RealmsMainScreen.this.font, s + "...", p_311349_, p_310646_, p_310447_);
            } else {
               p_311967_.drawString(RealmsMainScreen.this.font, p_310470_, p_311349_, p_310646_, p_310447_);
            }
         }
      }

      protected int versionTextX(int p_312234_, int p_313052_, Component p_311065_) {
         return p_312234_ + p_313052_ - RealmsMainScreen.this.font.width(p_311065_) - 20;
      }

      protected int gameModeTextX(int p_361673_, int p_369353_, Component p_362134_) {
         return p_361673_ + p_369353_ - RealmsMainScreen.this.font.width(p_362134_) - 20;
      }

      protected int renderGameMode(RealmsServer p_363303_, GuiGraphics p_363659_, int p_362703_, int p_363524_, int p_362657_) {
         boolean flag = p_363303_.isHardcore;
         int i = p_363303_.gameMode;
         int j = p_362703_;
         if (GameType.isValidId(i)) {
            Component component = RealmsMainScreen.getGameModeComponent(i, flag);
            j = this.gameModeTextX(p_362703_, p_363524_, component);
            p_363659_.drawString(RealmsMainScreen.this.font, component, j, this.secondLineY(p_362657_), -8355712);
         }

         if (flag) {
            j -= 10;
            p_363659_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, j, this.secondLineY(p_362657_), 8, 8);
         }

         return j;
      }

      protected int firstLineY(int p_311005_) {
         return p_311005_ + 1;
      }

      protected int lineHeight() {
         return 11;
      }

      protected int textX(int p_312460_) {
         return p_312460_ + 36 + 2;
      }

      protected int secondLineY(int p_309933_) {
         return p_309933_ + this.lineHeight();
      }

      protected int thirdLineY(int p_310502_) {
         return p_310502_ + this.lineHeight() * 2;
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum LayoutState {
      LOADING,
      NO_REALMS,
      LIST;
   }

   @OnlyIn(Dist.CLIENT)
   static class NotificationButton extends SpriteIconButton.CenteredIcon {
      private static final Identifier[] NOTIFICATION_ICONS = new Identifier[]{
         Identifier.withDefaultNamespace("notification/1"),
         Identifier.withDefaultNamespace("notification/2"),
         Identifier.withDefaultNamespace("notification/3"),
         Identifier.withDefaultNamespace("notification/4"),
         Identifier.withDefaultNamespace("notification/5"),
         Identifier.withDefaultNamespace("notification/more")
      };
      private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
      private static final int SIZE = 20;
      private static final int SPRITE_SIZE = 14;
      private int notificationCount;

      public NotificationButton(Component p_299660_, Identifier p_451640_, Button.OnPress p_297337_, @Nullable Component p_424529_) {
         super(20, 20, p_299660_, 14, 14, new WidgetSprites(p_451640_), p_297337_, p_424529_, null);
      }

      int notificationCount() {
         return this.notificationCount;
      }

      public void setNotificationCount(int p_300462_) {
         this.notificationCount = p_300462_;
      }

      @Override
      public void renderContents(GuiGraphics p_460483_, int p_454286_, int p_460257_, float p_456266_) {
         super.renderContents(p_460483_, p_454286_, p_460257_, p_456266_);
         if (this.active && this.notificationCount != 0) {
            this.drawNotificationCounter(p_460483_);
         }
      }

      private void drawNotificationCounter(GuiGraphics p_301365_) {
         p_301365_.blitSprite(
            RenderPipelines.GUI_TEXTURED, NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      public static final int PADDING = 7;
      public static final int HEIGHT_WITHOUT_TEXT = 38;
      private final Component text;
      private final List<AbstractWidget> children = new ArrayList<>();
      private final RealmsMainScreen.@Nullable CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private final Button button;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(
         final RealmsMainScreen p_425744_, final int p_301862_, final Component p_275215_, final RealmsNotification.VisitUrl p_426872_
      ) {
         this.text = p_275215_;
         this.gridLayout = new GridLayout();
         this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, p_301862_), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame
            .addChild(
               new MultiLineTextWidget(p_275215_, RealmsMainScreen.this.font).setCentered(true),
               this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
            );
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (p_426872_.dismissable()) {
            this.dismissButton = this.gridLayout
               .addChild(
                  new RealmsMainScreen.CrossButton(
                     p_420586_ -> RealmsMainScreen.this.dismissNotification(p_426872_.uuid()), Component.translatable("mco.notification.dismiss")
                  ),
                  0,
                  2,
                  this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
               );
         } else {
            this.dismissButton = null;
         }

         this.button = this.gridLayout
            .addChild(p_426872_.buildOpenLinkButton(p_425744_), 1, 1, this.gridLayout.newCellSettings().alignHorizontallyCenter().padding(4));
         this.button.setOverrideRenderHighlightedSprite(() -> this.isFocused());
         this.gridLayout.visitWidgets(this.children::add);
      }

      @Override
      public boolean keyPressed(KeyEvent p_424932_) {
         if (this.button.keyPressed(p_424932_)) {
            return true;
         } else {
            return this.dismissButton != null && this.dismissButton.keyPressed(p_424932_) ? true : super.keyPressed(p_424932_);
         }
      }

      private void updateEntryWidth() {
         int i = this.getWidth();
         if (this.lastEntryWidth != i) {
            this.refreshLayout(i);
            this.lastEntryWidth = i;
         }
      }

      private void refreshLayout(int p_275267_) {
         int i = textWidth(p_275267_);
         this.textFrame.setMinWidth(i);
         this.textWidget.setMaxWidth(i);
         this.gridLayout.arrangeElements();
      }

      public static int textWidth(int p_430375_) {
         return p_430375_ - 80;
      }

      @Override
      public void renderContent(GuiGraphics p_428410_, int p_422953_, int p_426529_, boolean p_424939_, float p_424583_) {
         this.gridLayout.setPosition(this.getContentX(), this.getContentY());
         this.updateEntryWidth();
         this.children.forEach(p_280688_ -> p_280688_.render(p_428410_, p_422953_, p_426529_, p_424583_));
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_427857_, boolean p_422940_) {
         if (this.dismissButton != null && this.dismissButton.mouseClicked(p_427857_, p_422940_)) {
            return true;
         } else {
            return this.button.mouseClicked(p_427857_, p_422940_) ? true : super.mouseClicked(p_427857_, p_422940_);
         }
      }

      public Component getText() {
         return this.text;
      }

      @Override
      public Component getNarration() {
         return this.getText();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ParentEntry extends RealmsMainScreen.Entry {
      private final RealmsServer server;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ParentEntry(final RealmsServer p_311143_) {
         this.server = p_311143_;
         if (!p_311143_.expired) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
         }
      }

      @Override
      public void renderContent(GuiGraphics p_424113_, int p_425174_, int p_427951_, boolean p_427286_, float p_430062_) {
         this.renderStatusLights(this.server, p_424113_, this.getContentRight(), this.getContentY(), p_425174_, p_427951_);
         RealmsUtil.renderPlayerFace(p_424113_, this.getContentX(), this.getContentY(), 32, this.server.ownerUUID);
         this.renderFirstLine(p_424113_, this.getContentY(), this.getContentX(), this.getContentWidth(), -8355712, this.server);
         this.renderSecondLine(p_424113_, this.getContentY(), this.getContentX(), this.getContentWidth(), this.server);
         this.renderThirdLine(p_424113_, this.getContentY(), this.getContentX(), this.server);
         this.tooltip
            .refreshTooltipForNextRenderPass(
               p_424113_,
               p_425174_,
               p_427951_,
               p_427286_,
               this.isFocused(),
               new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
            );
      }

      @Override
      public Component getNarration() {
         return Component.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends ObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(Minecraft.getInstance(), RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
      }

      public void setSelected(RealmsMainScreen.@Nullable Entry p_86849_) {
         super.setSelected(p_86849_);
         RealmsMainScreen.this.updateButtonStates();
      }

      @Override
      public int getRowWidth() {
         return 300;
      }

      void refreshEntries(RealmsMainScreen p_369757_) {
         RealmsMainScreen.Entry realmsmainscreen$entry = this.getSelected();
         this.clearEntries();

         for (RealmsNotification realmsnotification : RealmsMainScreen.this.notifications) {
            if (realmsnotification instanceof RealmsNotification.VisitUrl realmsnotification$visiturl) {
               this.addEntriesForNotification(realmsnotification$visiturl, p_369757_, realmsmainscreen$entry);
               RealmsMainScreen.this.markNotificationsAsSeen(List.of(realmsnotification));
               break;
            }
         }

         this.refreshServerEntries(realmsmainscreen$entry);
      }

      private void addEntriesForNotification(RealmsNotification.VisitUrl p_365197_, RealmsMainScreen p_363091_, RealmsMainScreen.@Nullable Entry p_423355_) {
         Component component = p_365197_.getMessage();
         int i = RealmsMainScreen.this.font.wordWrapHeight(component, RealmsMainScreen.NotificationMessageEntry.textWidth(this.getRowWidth()));
         RealmsMainScreen.NotificationMessageEntry realmsmainscreen$notificationmessageentry = RealmsMainScreen.this.new NotificationMessageEntry(
            p_363091_, i, component, p_365197_
         );
         this.addEntry(realmsmainscreen$notificationmessageentry, 38 + i);
         if (p_423355_ instanceof RealmsMainScreen.NotificationMessageEntry realmsmainscreen$notificationmessageentry1
            && realmsmainscreen$notificationmessageentry1.getText().equals(component)) {
            this.setSelected(realmsmainscreen$notificationmessageentry);
         }
      }

      private void refreshServerEntries(RealmsMainScreen.@Nullable Entry p_423732_) {
         for (RealmsServer realmsserver : RealmsMainScreen.this.availableSnapshotServers) {
            this.addEntry(RealmsMainScreen.this.new AvailableSnapshotEntry(realmsserver));
         }

         for (RealmsServer realmsserver1 : RealmsMainScreen.this.serverList) {
            RealmsMainScreen.Entry realmsmainscreen$entry;
            if (RealmsMainScreen.isSnapshot() && !realmsserver1.isSnapshotRealm()) {
               if (realmsserver1.state == RealmsServer.State.UNINITIALIZED) {
                  continue;
               }

               realmsmainscreen$entry = RealmsMainScreen.this.new ParentEntry(realmsserver1);
            } else {
               realmsmainscreen$entry = RealmsMainScreen.this.new ServerEntry(realmsserver1);
            }

            this.addEntry(realmsmainscreen$entry);
            if (p_423732_ instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
               && realmsmainscreen$serverentry.serverData.id == realmsserver1.id) {
               this.setSelected(realmsmainscreen$entry);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface RealmsCall<T> {
      T request(RealmsClient var1) throws RealmsServiceException;
   }

   @OnlyIn(Dist.CLIENT)
   class ServerEntry extends RealmsMainScreen.Entry {
      private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
      private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
      private static final int PLAYERS_ONLINE_SPRITE_SEPARATION = 3;
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      final RealmsServer serverData;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ServerEntry(final RealmsServer p_86856_) {
         this.serverData = p_86856_;
         boolean flag = RealmsMainScreen.isSelfOwnedServer(p_86856_);
         if (RealmsMainScreen.isSnapshot() && flag && p_86856_.isSnapshotRealm()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", p_86856_.parentWorldName)));
         } else if (!flag && p_86856_.needsDowngrade()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", p_86856_.activeVersion)));
         }
      }

      @Override
      public void renderContent(GuiGraphics p_427295_, int p_425049_, int p_427862_, boolean p_422830_, float p_425850_) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            p_427295_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
            int i = this.getContentYMiddle() - 4;
            p_427295_.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, this.getContentX() + 40 - 2, i, -8388737);
         } else {
            RealmsUtil.renderPlayerFace(p_427295_, this.getContentX(), this.getContentY(), 32, this.serverData.ownerUUID);
            this.renderFirstLine(p_427295_, this.getContentY(), this.getContentX(), this.getContentWidth(), -1, this.serverData);
            this.renderSecondLine(p_427295_, this.getContentY(), this.getContentX(), this.getContentWidth(), this.serverData);
            this.renderThirdLine(p_427295_, this.getContentY(), this.getContentX(), this.serverData);
            this.renderStatusLights(this.serverData, p_427295_, this.getContentRight(), this.getContentY(), p_425049_, p_427862_);
            boolean flag = this.renderOnlinePlayers(
               p_427295_, this.getContentY(), this.getContentX(), this.getContentWidth(), this.getContentHeight(), p_425049_, p_427862_, p_425850_
            );
            if (!flag) {
               this.tooltip
                  .refreshTooltipForNextRenderPass(
                     p_427295_,
                     p_425049_,
                     p_427862_,
                     p_422830_,
                     this.isFocused(),
                     new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
                  );
            }
         }
      }

      private boolean renderOnlinePlayers(
         GuiGraphics p_343952_, int p_344863_, int p_344728_, int p_342937_, int p_345144_, int p_344520_, int p_342333_, float p_407246_
      ) {
         List<ResolvableProfile> list = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
         int i = list.size();
         if (i > 0) {
            int j = p_344728_ + p_342937_ - 21;
            int k = p_344863_ + p_345144_ - 9 - 2;
            int l = 9 * i + 3 * (i - 1);
            int i1 = j - l;
            List<PlayerSkinRenderCache.RenderInfo> list1;
            if (p_344520_ >= i1 && p_344520_ <= j && p_342333_ >= k && p_342333_ <= k + 9) {
               list1 = new ArrayList<>(i);
            } else {
               list1 = null;
            }

            PlayerSkinRenderCache playerskinrendercache = RealmsMainScreen.this.minecraft.playerSkinRenderCache();

            for (int j1 = 0; j1 < list.size(); j1++) {
               ResolvableProfile resolvableprofile = list.get(j1);
               PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = playerskinrendercache.getOrDefault(resolvableprofile);
               int k1 = i1 + 12 * j1;
               PlayerFaceRenderer.draw(p_343952_, playerskinrendercache$renderinfo.playerSkin(), k1, k, 9);
               if (list1 != null) {
                  list1.add(playerskinrendercache$renderinfo);
               }
            }

            if (list1 != null) {
               p_343952_.setTooltipForNextFrame(
                  RealmsMainScreen.this.font,
                  List.of(ONLINE_PLAYERS_TOOLTIP_HEADER),
                  Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip(list1)),
                  p_344520_,
                  p_342333_
               );
               return true;
            }
         }

         return false;
      }

      private void playRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
      }

      private void createUnitializedRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(
            RealmsMainScreen.this, this.serverData, this.serverData.isSnapshotRealm()
         );
         RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_424374_, boolean p_427374_) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            this.createUnitializedRealm();
         } else if (this.serverData.shouldPlayButtonBeActive() && p_427374_ && this.isFocused()) {
            this.playRealm();
         }

         return true;
      }

      @Override
      public boolean keyPressed(KeyEvent p_427360_) {
         if (p_427360_.isSelection()) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
               this.createUnitializedRealm();
               return true;
            }

            if (this.serverData.shouldPlayButtonBeActive()) {
               this.playRealm();
               return true;
            }
         }

         return super.keyPressed(p_427360_);
      }

      @Override
      public Component getNarration() {
         return this.serverData.state == RealmsServer.State.UNINITIALIZED
            ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
            : Component.translatable("narrator.select", Objects.requireNonNullElse(this.serverData.name, "unknown server"));
      }

      public RealmsServer getServer() {
         return this.serverData;
      }
   }
}
