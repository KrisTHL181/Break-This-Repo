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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface LoggedChatMessage extends LoggedChatEvent {
   static LoggedChatMessage.Player player(GameProfile p_261832_, PlayerChatMessage p_261491_, ChatTrustLevel p_262141_) {
      return new LoggedChatMessage.Player(p_261832_, p_261491_, p_262141_);
   }

   static LoggedChatMessage.System system(Component p_242325_, Instant p_242334_) {
      return new LoggedChatMessage.System(p_242325_, p_242334_);
   }

   Component toContentComponent();

   default Component toNarrationComponent() {
      return this.toContentComponent();
   }

   boolean canReport(UUID var1);

   @OnlyIn(Dist.CLIENT)
   record Player(GameProfile profile, PlayerChatMessage message, ChatTrustLevel trustLevel) implements LoggedChatMessage {
      public static final MapCodec<LoggedChatMessage.Player> CODEC = RecordCodecBuilder.mapCodec(
         p_420857_ -> p_420857_.group(
               ExtraCodecs.AUTHLIB_GAME_PROFILE.fieldOf("profile").forGetter(LoggedChatMessage.Player::profile),
               PlayerChatMessage.MAP_CODEC.forGetter(LoggedChatMessage.Player::message),
               ChatTrustLevel.CODEC.optionalFieldOf("trust_level", ChatTrustLevel.SECURE).forGetter(LoggedChatMessage.Player::trustLevel)
            )
            .apply(p_420857_, LoggedChatMessage.Player::new)
      );
      private static final DateTimeFormatter TIME_FORMATTER = Util.localizedDateFormatter(FormatStyle.SHORT);

      @Override
      public Component toContentComponent() {
         if (!this.message.filterMask().isEmpty()) {
            Component component = this.message.filterMask().applyWithFormatting(this.message.signedContent());
            return component != null ? component : Component.empty();
         } else {
            return this.message.decoratedContent();
         }
      }

      @Override
      public Component toNarrationComponent() {
         Component component = this.toContentComponent();
         Component component1 = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.message.narrate", this.profile.name(), component, component1);
      }

      public Component toHeadingComponent() {
         Component component = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.heading", this.profile.name(), component);
      }

      private Component getTimeComponent() {
         ZonedDateTime zoneddatetime = ZonedDateTime.ofInstant(this.message.timeStamp(), ZoneId.systemDefault());
         return Component.literal(zoneddatetime.format(TIME_FORMATTER)).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
      }

      @Override
      public boolean canReport(UUID p_242210_) {
         return this.message.hasSignatureFrom(p_242210_);
      }

      public UUID profileId() {
         return this.profile.id();
      }

      @Override
      public LoggedChatEvent.Type type() {
         return LoggedChatEvent.Type.PLAYER;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record System(Component message, Instant timeStamp) implements LoggedChatMessage {
      public static final MapCodec<LoggedChatMessage.System> CODEC = RecordCodecBuilder.mapCodec(
         p_308279_ -> p_308279_.group(
               ComponentSerialization.CODEC.fieldOf("message").forGetter(LoggedChatMessage.System::message),
               ExtraCodecs.INSTANT_ISO8601.fieldOf("time_stamp").forGetter(LoggedChatMessage.System::timeStamp)
            )
            .apply(p_308279_, LoggedChatMessage.System::new)
      );

      @Override
      public Component toContentComponent() {
         return this.message;
      }

      @Override
      public boolean canReport(UUID p_242173_) {
         return false;
      }

      @Override
      public LoggedChatEvent.Type type() {
         return LoggedChatEvent.Type.SYSTEM;
      }
   }
}
