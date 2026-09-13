package net.minecraft.advancements;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

public enum AdvancementType implements StringRepresentable {
   TASK("task", ChatFormatting.GREEN),
   CHALLENGE("challenge", ChatFormatting.DARK_PURPLE),
   GOAL("goal", ChatFormatting.GREEN);

   public static final Codec<AdvancementType> CODEC = StringRepresentable.fromEnum(AdvancementType::values);
   private final String name;
   private final ChatFormatting chatColor;
   private final Component displayName;

   AdvancementType(final String p_309637_, final ChatFormatting p_312188_) {
      this.name = p_309637_;
      this.chatColor = p_312188_;
      this.displayName = Component.translatable("advancements.toast." + p_309637_);
   }

   public ChatFormatting getChatColor() {
      return this.chatColor;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public MutableComponent createAnnouncement(AdvancementHolder p_311620_, ServerPlayer p_311407_) {
      return Component.translatable("chat.type.advancement." + this.name, p_311407_.getDisplayName(), Advancement.name(p_311620_));
   }
}
