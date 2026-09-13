package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum NarratorStatus {
   OFF(0, "options.narrator.off"),
   ALL(1, "options.narrator.all"),
   CHAT(2, "options.narrator.chat"),
   SYSTEM(3, "options.narrator.system");

   private static final IntFunction<NarratorStatus> BY_ID = ByIdMap.continuous(NarratorStatus::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<NarratorStatus> LEGACY_CODEC = Codec.INT.xmap(NarratorStatus::byId, NarratorStatus::getId);
   private final int id;
   private final Component name;

   NarratorStatus(final int p_91616_, final String p_91617_) {
      this.id = p_91616_;
      this.name = Component.translatable(p_91617_);
   }

   public int getId() {
      return this.id;
   }

   public Component getName() {
      return this.name;
   }

   public static NarratorStatus byId(int p_91620_) {
      return BY_ID.apply(p_91620_);
   }

   public boolean shouldNarrateChat() {
      return this == ALL || this == CHAT;
   }

   public boolean shouldNarrateSystem() {
      return this == ALL || this == SYSTEM;
   }

   public boolean shouldNarrateSystemOrChat() {
      return this == ALL || this == SYSTEM || this == CHAT;
   }
}
