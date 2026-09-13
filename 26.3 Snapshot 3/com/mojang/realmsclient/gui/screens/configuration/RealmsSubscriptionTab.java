package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;

public class RealmsSubscriptionTab extends GridLayoutTab implements RealmsConfigurationTab {
   private static final int DEFAULT_COMPONENT_WIDTH = 200;
   private static final int EXTRA_SPACING = 2;
   private static final int DEFAULT_SPACING = 6;
   public static final Component TITLE = Component.translatable("mco.configure.world.subscription.tab");
   private static final Component SUBSCRIPTION_START_LABEL = Component.translatable("mco.configure.world.subscription.start");
   private static final Component TIME_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.timeleft");
   private static final Component DAYS_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.recurring.daysleft");
   private static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.configure.world.subscription.expired").withStyle(ChatFormatting.GRAY);
   private static final Component SUBSCRIPTION_LESS_THAN_A_DAY_TEXT = Component.translatable("mco.configure.world.subscription.less_than_a_day")
      .withStyle(ChatFormatting.GRAY);
   private static final Component RECURRING_INFO = Component.translatable("mco.configure.world.subscription.recurring.info");
   private final RealmsConfigureWorldScreen configurationScreen;
   private final Minecraft minecraft;
   private final Button deleteButton;
   private final FocusableTextWidget subscriptionInfo;
   private final Component startDate;
   private final Component daysLeft;
   private RealmsServer serverData;

   public RealmsSubscriptionTab(
      final RealmsConfigureWorldScreen configurationScreen, final Minecraft minecraft, final RealmsServer serverData, final Subscription subscription
   ) {
      super(TITLE);
      this.configurationScreen = configurationScreen;
      this.minecraft = minecraft;
      this.serverData = serverData;
      this.startDate = localPresentation(subscription.startDate());
      this.daysLeft = this.daysLeftPresentation(subscription.daysLeft());
      GridLayout.RowHelper helper = this.layout.rowSpacing(6).createRowHelper(1);
      Font font = configurationScreen.getFont();
      helper.addChild(new StringWidget(200, 9, SUBSCRIPTION_START_LABEL, font));
      helper.addChild(new StringWidget(200, 9, this.startDate, font));
      helper.addChild(SpacerElement.height(2));
      Component daysLeftLabel = subscription.type() == Subscription.SubscriptionType.RECURRING ? DAYS_LEFT_LABEL : TIME_LEFT_LABEL;
      helper.addChild(new StringWidget(200, 9, daysLeftLabel, font));
      helper.addChild(new StringWidget(200, 9, this.daysLeft, font));
      helper.addChild(SpacerElement.height(2));
      helper.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.subscription.extend"),
               button -> ConfirmLinkScreen.confirmLinkNow(
                  configurationScreen, CommonLinks.extendRealms(serverData.remoteSubscriptionId, minecraft.getUser().getProfileId())
               )
            )
            .bounds(0, 0, 200, 20)
            .build()
      );
      helper.addChild(SpacerElement.height(2));
      this.deleteButton = helper.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.delete.button"),
               var3 -> minecraft.gui
                  .setScreen(
                     RealmsPopups.warningPopupScreen(
                        configurationScreen, Component.translatable("mco.configure.world.delete.question.line1"), popup -> this.deleteRealm()
                     )
                  )
            )
            .bounds(0, 0, 200, 20)
            .build()
      );
      helper.addChild(SpacerElement.height(2));
      this.subscriptionInfo = helper.addChild(
         FocusableTextWidget.builder(Component.empty(), font).maxWidth(200).build(), LayoutSettings.defaults().alignHorizontallyCenter()
      );
      this.subscriptionInfo.setCentered(false);
      this.updateData(serverData);
   }

   private void deleteRealm() {
      RealmsUtil.runAsync(
            client -> client.deleteRealm(this.serverData.id),
            RealmsUtil.openScreenAndLogOnFailure(this.configurationScreen::createErrorScreen, "Couldn't delete world")
         )
         .thenRunAsync(() -> this.minecraft.gui.setScreen(this.configurationScreen.getLastScreen()), this.minecraft);
      this.minecraft.gui.setScreen(this.configurationScreen);
   }

   private static Component localPresentation(final Instant time) {
      String formattedDate = ZonedDateTime.ofInstant(time, ZoneId.systemDefault()).format(Util.localizedDateFormatter(FormatStyle.MEDIUM));
      return Component.literal(formattedDate).withStyle(ChatFormatting.GRAY);
   }

   private Component daysLeftPresentation(final int daysLeft) {
      if (daysLeft < 0 && this.serverData.expired) {
         return SUBSCRIPTION_EXPIRED_TEXT;
      } else if (daysLeft <= 1) {
         return SUBSCRIPTION_LESS_THAN_A_DAY_TEXT;
      } else {
         int months = daysLeft / 30;
         int days = daysLeft % 30;
         boolean showMonths = months > 0;
         boolean showDays = days > 0;
         if (showMonths && showDays) {
            return Component.translatable("mco.configure.world.subscription.remaining.months.days", months, days).withStyle(ChatFormatting.GRAY);
         } else if (showMonths) {
            return Component.translatable("mco.configure.world.subscription.remaining.months", months).withStyle(ChatFormatting.GRAY);
         } else {
            return showDays
               ? Component.translatable("mco.configure.world.subscription.remaining.days", days).withStyle(ChatFormatting.GRAY)
               : Component.empty();
         }
      }
   }

   @Override
   public void updateData(final RealmsServer serverData) {
      this.serverData = serverData;
      boolean snapshotWorld = RealmsMainScreen.isSnapshot() && serverData.parentWorldName != null;
      this.deleteButton.active = serverData.expired;
      if (snapshotWorld) {
         this.subscriptionInfo.setMessage(Component.translatable("mco.snapshot.subscription.info", serverData.parentWorldName));
      } else {
         this.subscriptionInfo.setMessage(RECURRING_INFO);
      }

      this.layout.arrangeElements();
   }

   @Override
   public Component getTabExtraNarration() {
      return CommonComponents.joinLines(TITLE, SUBSCRIPTION_START_LABEL, this.startDate, TIME_LEFT_LABEL, this.daysLeft);
   }
}
