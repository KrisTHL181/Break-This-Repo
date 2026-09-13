package net.minecraft.client.gui.screens.options;

import com.mojang.authlib.services.FriendsService.ResultCode;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PrivacyConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.FriendsListConfirmScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import org.jspecify.annotations.Nullable;

public class OnlineOptionsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.online.title");
   private static final Component SERVERS_HEADER = Component.translatable("options.online.servers.header");
   private static final Component REALMS_HEADER = Component.translatable("options.online.realms.header");
   private static final Component FRIENDS_HEADER = Component.translatable("options.online.friends.header");
   private static final Component XBOX_SETTINGS = Component.translatable("options.online.xboxSettings");
   private static final Component FRIENDS_CONFIRM_TITLE = Component.translatable("options.friendsList.confirm.title").withStyle(ChatFormatting.UNDERLINE);
   private static final Component MICROSOFT_ACCOUNT_LINK = Component.translatable("options.friendsList.confirm.message.link")
      .withStyle(
         style -> style.withUnderlined(true).withColor(ChatFormatting.BLUE).withClickEvent(new ClickEvent.OpenUrl(CommonLinks.PRIVACY_AND_ONLINE_SETTINGS))
      );
   private static final Component FRIENDS_CONFIRM_MESSAGE = Component.translatable("options.friendsList.confirm.message", MICROSOFT_ACCOUNT_LINK);
   private static final Component FRIENDS_CONFIRM_TURN_ON = Component.translatable("options.friendsList.confirm.turnOn");
   private static final Component FRIENDS_CONFIRM_TURN_OFF = Component.translatable("options.friendsList.confirm.turnOff");
   private static final Component FRIENDS_LIST_LABEL = Component.translatable("options.friendsList");
   private static final Component ALLOW_FRIEND_REQUESTS_LABEL = Component.translatable("options.allowFriendRequests");
   private static final Tooltip ALLOW_FRIEND_REQUESTS_TOOLTIP = Tooltip.create(Component.translatable("options.allowFriendRequests.tooltip"));
   private static final Component IN_GAME_NOTIFICATIONS_LABEL = Component.translatable("options.inGameNotification");
   private static final Tooltip IN_GAME_NOTIFICATIONS_TOOLTIP = Tooltip.create(Component.translatable("options.inGameNotification.tooltip"));
   private @Nullable CycleButton<Boolean> friendsListButton;
   private @Nullable CycleButton<Boolean> allowFriendRequestsButton;
   private @Nullable CycleButton<Boolean> inGameNotificationButton;
   private @Nullable AbstractWidget presenceWidget;

   public OnlineOptionsScreen(final Screen lastScreen, final Options options) {
      super(lastScreen, options, TITLE);
   }

   public static void confirmFriendsListEnabled(final Minecraft minecraft, final Runnable onEnabled, final @Nullable Screen lastScreen) {
      PlayerSocialManager playerSocialManager = minecraft.getPlayerSocialManager();
      if (playerSocialManager.isFriendListEnabled()) {
         onEnabled.run();
      } else {
         minecraft.gui.setScreen(new FriendsListConfirmScreen(accepted -> {
            if (accepted) {
               applyFriendSettings(minecraft, true, true, successful -> {
                  if (successful) {
                     onEnabled.run();
                  }
               });
            } else {
               minecraft.gui.setScreen(lastScreen);
            }
         }, FRIENDS_CONFIRM_TITLE, FRIENDS_CONFIRM_MESSAGE, FRIENDS_CONFIRM_TURN_ON, FRIENDS_CONFIRM_TURN_OFF));
      }
   }

   private static void applyFriendSettings(
      final Minecraft minecraft, final boolean friendsListEnabled, final boolean allowFriendRequests, final Consumer<Boolean> onResult
   ) {
      PlayerSocialManager playerSocialManager = minecraft.getPlayerSocialManager();
      playerSocialManager.updateFriendSettings(friendsListEnabled, allowFriendRequests).whenCompleteAsync((result, var5) -> {
         boolean success = result == ResultCode.SUCCESS;
         if (success) {
            playerSocialManager.setFriendListEnabled(friendsListEnabled);
            playerSocialManager.setAllowFriendRequests(allowFriendRequests);
         }

         onResult.accept(success);
      }, minecraft);
   }

   @Override
   protected void addOptions() {
      this.list.addHeader(FRIENDS_HEADER);
      PlayerSocialManager playerSocialManager = this.minecraft.getPlayerSocialManager();
      OptionInstance<Boolean> inGameNotificationOpt = this.options.inGameNotification();
      this.friendsListButton = CycleButton.onOffBuilder(playerSocialManager.isFriendListEnabled())
         .create(0, 0, 150, 20, FRIENDS_LIST_LABEL, (var3, newValue) -> this.onFriendsListToggled(newValue, playerSocialManager, inGameNotificationOpt));
      this.friendsListButton.active = !this.minecraft.isDemo() && !this.minecraft.isOfflineDeveloperMode();
      this.allowFriendRequestsButton = CycleButton.onOffBuilder(playerSocialManager.isAllowFriendRequests())
         .withTooltip(var0 -> ALLOW_FRIEND_REQUESTS_TOOLTIP)
         .create(
            0,
            0,
            150,
            20,
            ALLOW_FRIEND_REQUESTS_LABEL,
            (var2x, enabled) -> applyFriendSettings(
               this.minecraft, playerSocialManager.isFriendListEnabled(), enabled, var1x -> this.updateFriendListDependentButtons()
            )
         );
      this.list.addSmall(this.friendsListButton, this.allowFriendRequestsButton);
      this.inGameNotificationButton = CycleButton.onOffBuilder(inGameNotificationOpt.get())
         .withTooltip(var0 -> IN_GAME_NOTIFICATIONS_TOOLTIP)
         .create(0, 0, 150, 20, IN_GAME_NOTIFICATIONS_LABEL, (var1x, enabled) -> inGameNotificationOpt.set(enabled));
      this.presenceWidget = this.options.sharePresence().createButton(this.options);
      this.list.addSmall(this.inGameNotificationButton, this.presenceWidget);
      this.updateFriendListDependentButtons();
      this.list.addBig(Button.builder(XBOX_SETTINGS, var1x -> PrivacyConfirmLinkScreen.confirmLinkNow(this, CommonLinks.PRIVACY_AND_ONLINE_SETTINGS)).build());
      this.list.addHeader(SERVERS_HEADER);
      this.list.addBig(this.options.allowServerListing());
      this.list.addHeader(REALMS_HEADER);
      this.list.addBig(this.options.realmsNotifications());
   }

   private void onFriendsListToggled(final Boolean newValue, final PlayerSocialManager playerSocialManager, final OptionInstance<Boolean> inGameNotificationOpt) {
      if (newValue) {
         this.minecraft.gui.setScreen(new FriendsListConfirmScreen(accepted -> {
            this.minecraft.gui.setScreen(this);
            if (accepted && !this.minecraft.isOfflineDeveloperMode()) {
               playerSocialManager.setFriendListEnabled(true);
               playerSocialManager.setAllowFriendRequests(true);
               applyFriendSettings(this.minecraft, true, true, var1x -> this.updateFriendListDependentButtons());
            }

            this.updateFriendListDependentButtons();
         }, FRIENDS_CONFIRM_TITLE, FRIENDS_CONFIRM_MESSAGE, FRIENDS_CONFIRM_TURN_ON, FRIENDS_CONFIRM_TURN_OFF));
      } else {
         boolean friendListEnabled = playerSocialManager.isFriendListEnabled();
         boolean allowFriendRequests = playerSocialManager.isAllowFriendRequests();
         playerSocialManager.setFriendListEnabled(false);
         playerSocialManager.setAllowFriendRequests(false);
         inGameNotificationOpt.set(false);
         this.updateFriendListDependentButtons();
         applyFriendSettings(this.minecraft, false, false, result -> {
            if (!result) {
               playerSocialManager.setFriendListEnabled(friendListEnabled);
               playerSocialManager.setAllowFriendRequests(allowFriendRequests);
            }

            this.updateFriendListDependentButtons();
         });
      }
   }

   private void updateFriendListDependentButtons() {
      PlayerSocialManager playerSocialManager = this.minecraft.getPlayerSocialManager();
      boolean enabled = playerSocialManager.isFriendListEnabled();
      if (this.friendsListButton != null) {
         this.friendsListButton.setValue(enabled);
      }

      if (this.allowFriendRequestsButton != null) {
         this.allowFriendRequestsButton.setValue(playerSocialManager.isAllowFriendRequests());
         this.allowFriendRequestsButton.active = enabled;
      }

      if (this.inGameNotificationButton != null) {
         this.inGameNotificationButton.setValue(this.options.inGameNotification().get());
         this.inGameNotificationButton.active = enabled;
      }

      if (this.presenceWidget != null) {
         this.presenceWidget.active = enabled;
      }
   }
}
