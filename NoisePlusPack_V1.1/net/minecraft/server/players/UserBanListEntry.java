package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.util.Date;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class UserBanListEntry extends BanListEntry<NameAndId> {
   private static final Component MESSAGE_UNKNOWN_USER = Component.translatable("commands.banlist.entry.unknown");

   public UserBanListEntry(@Nullable NameAndId p_425518_) {
      this(p_425518_, null, null, null, null);
   }

   public UserBanListEntry(
      @Nullable NameAndId p_427215_, @Nullable Date p_11439_, @Nullable String p_11440_, @Nullable Date p_11441_, @Nullable String p_11442_
   ) {
      super(p_427215_, p_11439_, p_11440_, p_11441_, p_11442_);
   }

   public UserBanListEntry(JsonObject p_11434_) {
      super(NameAndId.fromJson(p_11434_), p_11434_);
   }

   @Override
   protected void serialize(JsonObject p_11444_) {
      if (this.getUser() != null) {
         this.getUser().appendTo(p_11444_);
         super.serialize(p_11444_);
      }
   }

   @Override
   public Component getDisplayName() {
      NameAndId nameandid = this.getUser();
      return nameandid != null ? Component.literal(nameandid.name()) : MESSAGE_UNKNOWN_USER;
   }
}
