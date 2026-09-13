package net.minecraft.client.multiplayer.chat;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;

public interface LoggedChatMessage extends LoggedChatEvent {
   static LoggedChatMessage.Player player(final GameProfile profile, final PlayerChatMessage message, final ChatTrustLevel trustLevel) {
      return new LoggedChatMessage.Player(profile, message, trustLevel);
   }

   static LoggedChatMessage.System system(final Component message, final Instant timeStamp) {
      return new LoggedChatMessage.System(message, timeStamp);
   }

   Component toContentComponent();

   default Component toNarrationComponent() {
      return this.toContentComponent();
   }

   boolean canReport(UUID reportedPlayerId);

   record Player(GameProfile profile, PlayerChatMessage message, ChatTrustLevel trustLevel) implements LoggedChatMessage {
      public static final MapCodec<LoggedChatMessage.Player> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ExtraCodecs.AUTHLIB_GAME_PROFILE.fieldOf("profile").forGetter(LoggedChatMessage.Player::profile),
               PlayerChatMessage.MAP_CODEC.forGetter(LoggedChatMessage.Player::message),
               ChatTrustLevel.CODEC.optionalFieldOf("trust_level", ChatTrustLevel.SECURE).forGetter(LoggedChatMessage.Player::trustLevel)
            )
            .apply(i, LoggedChatMessage.Player::new)
      );
      private static final DateTimeFormatter TIME_FORMATTER = Util.localizedDateFormatter(FormatStyle.SHORT);

      @Override
      public Component toContentComponent() {
         if (!this.message.filterMask().isEmpty()) {
            Component filtered = this.message.filterMask().applyWithFormatting(this.message.signedContent());
            return filtered != null ? filtered : Component.empty();
         } else {
            return this.message.decoratedContent();
         }
      }

      @Override
      public Component toNarrationComponent() {
         Component content = this.toContentComponent();
         Component time = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.message.narrate", this.profile.name(), content, time);
      }

      public Component toHeadingComponent() {
         Component time = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.heading", this.profile.name(), time);
      }

      private Component getTimeComponent() {
         ZonedDateTime dateTime = ZonedDateTime.ofInstant(this.message.timeStamp(), ZoneId.systemDefault());
         return Component.literal(dateTime.format(TIME_FORMATTER)).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
      }

      @Override
      public boolean canReport(final UUID reportedPlayerId) {
         return this.message.hasSignatureFrom(reportedPlayerId);
      }

      public UUID profileId() {
         return this.profile.id();
      }

      @Override
      public LoggedChatEvent.Type type() {
         return LoggedChatEvent.Type.PLAYER;
      }
   }

   record System(Component message, Instant timeStamp) implements LoggedChatMessage {
      public static final MapCodec<LoggedChatMessage.System> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ComponentSerialization.CODEC.fieldOf("message").forGetter(LoggedChatMessage.System::message),
               ExtraCodecs.INSTANT_ISO8601.fieldOf("time_stamp").forGetter(LoggedChatMessage.System::timeStamp)
            )
            .apply(i, LoggedChatMessage.System::new)
      );

      @Override
      public Component toContentComponent() {
         return this.message;
      }

      @Override
      public boolean canReport(final UUID reportedPlayerId) {
         return false;
      }

      @Override
      public LoggedChatEvent.Type type() {
         return LoggedChatEvent.Type.SYSTEM;
      }
   }
}
