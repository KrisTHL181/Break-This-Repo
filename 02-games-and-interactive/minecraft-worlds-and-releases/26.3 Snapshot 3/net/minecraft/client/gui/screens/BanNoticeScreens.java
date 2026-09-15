package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.report.BanReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;

public class BanNoticeScreens {
   private static final Component TEMPORARY_BAN_TITLE = Component.translatable("gui.banned.title.temporary").withStyle(ChatFormatting.BOLD);
   private static final Component PERMANENT_BAN_TITLE = Component.translatable("gui.banned.title.permanent").withStyle(ChatFormatting.BOLD);
   public static final Component NAME_BAN_TITLE = Component.translatable("gui.banned.name.title").withStyle(ChatFormatting.BOLD);
   private static final Component SKIN_BAN_TITLE = Component.translatable("gui.banned.skin.title").withStyle(ChatFormatting.BOLD);
   private static final Component SKIN_BAN_DESCRIPTION = Component.translatable(
      "gui.banned.skin.description", Component.translationArg(CommonLinks.SUSPENSION_HELP)
   );

   public static ConfirmLinkScreen create(final BooleanConsumer callback, final BanDetails multiplayerBanned) {
      return new ConfirmLinkScreen(
         callback,
         getBannedTitle(multiplayerBanned),
         getBannedScreenText(multiplayerBanned),
         CommonLinks.SUSPENSION_HELP,
         CommonComponents.GUI_ACKNOWLEDGE,
         true
      );
   }

   public static ConfirmLinkScreen createSkinBan(final Runnable onClose) {
      URI uri = CommonLinks.SUSPENSION_HELP;
      return new ConfirmLinkScreen(result -> {
         if (result) {
            Util.getPlatform().openUri(uri);
         }

         onClose.run();
      }, SKIN_BAN_TITLE, SKIN_BAN_DESCRIPTION, uri, CommonComponents.GUI_ACKNOWLEDGE, true);
   }

   public static ConfirmLinkScreen createNameBan(final String name, final Runnable onClose) {
      URI uri = CommonLinks.SUSPENSION_HELP;
      return new ConfirmLinkScreen(
         result -> {
            if (result) {
               Util.getPlatform().openUri(uri);
            }

            onClose.run();
         },
         NAME_BAN_TITLE,
         Component.translatable(
            "gui.banned.name.description", Component.literal(name).withStyle(ChatFormatting.YELLOW), Component.translationArg(CommonLinks.SUSPENSION_HELP)
         ),
         uri,
         CommonComponents.GUI_ACKNOWLEDGE,
         true
      );
   }

   private static Component getBannedTitle(final BanDetails multiplayerBanned) {
      return isTemporaryBan(multiplayerBanned) ? TEMPORARY_BAN_TITLE : PERMANENT_BAN_TITLE;
   }

   private static Component getBannedScreenText(final BanDetails multiplayerBanned) {
      return Component.translatable(
         "gui.banned.description",
         getBanReasonText(multiplayerBanned),
         getBanStatusText(multiplayerBanned),
         Component.translationArg(CommonLinks.SUSPENSION_HELP)
      );
   }

   private static Component getBanReasonText(final BanDetails multiplayerBanned) {
      String reasonString = multiplayerBanned.reason();
      String reasonMessage = multiplayerBanned.reasonMessage();
      if (StringUtils.isNumeric(reasonString)) {
         int reasonId = Integer.parseInt(reasonString);
         BanReason reason = BanReason.byId(reasonId);
         Component reasonText;
         if (reason != null) {
            reasonText = ComponentUtils.mergeStyles(reason.title(), Style.EMPTY.withBold(true));
         } else if (reasonMessage != null) {
            reasonText = Component.translatable("gui.banned.description.reason_id_message", reasonId, reasonMessage).withStyle(ChatFormatting.BOLD);
         } else {
            reasonText = Component.translatable("gui.banned.description.reason_id", reasonId).withStyle(ChatFormatting.BOLD);
         }

         return Component.translatable("gui.banned.description.reason", reasonText);
      } else {
         return Component.translatable("gui.banned.description.unknownreason");
      }
   }

   private static Component getBanStatusText(final BanDetails multiplayerBanned) {
      if (isTemporaryBan(multiplayerBanned)) {
         Component banDurationText = getBanDurationText(multiplayerBanned);
         return Component.translatable(
            "gui.banned.description.temporary",
            Component.translatable("gui.banned.description.temporary.duration", banDurationText).withStyle(ChatFormatting.BOLD)
         );
      } else {
         return Component.translatable("gui.banned.description.permanent").withStyle(ChatFormatting.BOLD);
      }
   }

   private static Component getBanDurationText(final BanDetails multiplayerBanned) {
      Duration banDuration = Duration.between(Instant.now(), multiplayerBanned.expires());
      long durationHours = banDuration.toHours();
      if (durationHours > 72L) {
         return CommonComponents.days(banDuration.toDays());
      } else {
         return durationHours < 1L ? CommonComponents.minutes(banDuration.toMinutes()) : CommonComponents.hours(banDuration.toHours());
      }
   }

   private static boolean isTemporaryBan(final BanDetails multiplayerBanned) {
      return multiplayerBanned.expires() != null;
   }
}
