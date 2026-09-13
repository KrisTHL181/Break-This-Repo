package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public enum ChatTrustLevel implements StringRepresentable {
   SECURE("secure"),
   MODIFIED("modified"),
   NOT_SECURE("not_secure");

   public static final Codec<ChatTrustLevel> CODEC = StringRepresentable.fromEnum(ChatTrustLevel::values);
   private final String serializedName;

   ChatTrustLevel(final String serializedName) {
      this.serializedName = serializedName;
   }

   public static ChatTrustLevel evaluate(final PlayerChatMessage message, final Component decoratedMessage, final Instant received) {
      if (!message.hasSignature() || message.hasExpiredClient(received)) {
         return NOT_SECURE;
      } else {
         return isModified(message, decoratedMessage) ? MODIFIED : SECURE;
      }
   }

   private static boolean isModified(final PlayerChatMessage message, final Component decoratedMessage) {
      if (!decoratedMessage.getString().contains(message.signedContent())) {
         return true;
      }

      Component decoratedContent = message.unsignedContent();
      return decoratedContent == null ? false : containsModifiedStyle(decoratedContent);
   }

   private static boolean containsModifiedStyle(final Component decoratedContent) {
      return decoratedContent.<Boolean>visit((style, contents) -> isModifiedStyle(style) ? Optional.of(true) : Optional.empty(), Style.EMPTY).orElse(false);
   }

   private static boolean isModifiedStyle(final Style style) {
      return !style.getFont().equals(FontDescription.DEFAULT);
   }

   public boolean isNotSecure() {
      return this == NOT_SECURE;
   }

   public @Nullable GuiMessageTag createTag(final PlayerChatMessage message) {
      return switch (this) {
         case MODIFIED -> GuiMessageTag.chatModified(message.signedContent());
         case NOT_SECURE -> GuiMessageTag.chatNotSecure();
         default -> null;
      };
   }

   @Override
   public String getSerializedName() {
      return this.serializedName;
   }
}
