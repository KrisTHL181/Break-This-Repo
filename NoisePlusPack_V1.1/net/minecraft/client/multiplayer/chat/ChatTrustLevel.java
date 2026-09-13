package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public enum ChatTrustLevel implements StringRepresentable {
   SECURE("secure"),
   MODIFIED("modified"),
   NOT_SECURE("not_secure");

   public static final Codec<ChatTrustLevel> CODEC = StringRepresentable.fromEnum(ChatTrustLevel::values);
   private final String serializedName;

   ChatTrustLevel(final String p_254190_) {
      this.serializedName = p_254190_;
   }

   public static ChatTrustLevel evaluate(PlayerChatMessage p_248663_, Component p_248544_, Instant p_252024_) {
      if (p_248663_.hasSignature() && !p_248663_.hasExpiredClient(p_252024_)) {
         return isModified(p_248663_, p_248544_) ? MODIFIED : SECURE;
      } else {
         return NOT_SECURE;
      }
   }

   private static boolean isModified(PlayerChatMessage p_252093_, Component p_250811_) {
      if (!p_250811_.getString().contains(p_252093_.signedContent())) {
         return true;
      }

      Component component = p_252093_.unsignedContent();
      return component == null ? false : containsModifiedStyle(component);
   }

   private static boolean containsModifiedStyle(Component p_251011_) {
      return p_251011_.<Boolean>visit((p_251711_, p_250844_) -> isModifiedStyle(p_251711_) ? Optional.of(true) : Optional.empty(), Style.EMPTY).orElse(false);
   }

   private static boolean isModifiedStyle(Style p_251347_) {
      return !p_251347_.getFont().equals(FontDescription.DEFAULT);
   }

   public boolean isNotSecure() {
      return this == NOT_SECURE;
   }

   public @Nullable GuiMessageTag createTag(PlayerChatMessage p_240632_) {
      return switch (this) {
         case MODIFIED -> GuiMessageTag.chatModified(p_240632_.signedContent());
         case NOT_SECURE -> GuiMessageTag.chatNotSecure();
         default -> null;
      };
   }

   @Override
   public String getSerializedName() {
      return this.serializedName;
   }
}
