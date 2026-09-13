package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

public enum NarratorStatus {
   OFF(0, "options.narrator.off"),
   ALL(1, "options.narrator.all"),
   CHAT(2, "options.narrator.chat"),
   SYSTEM(3, "options.narrator.system");

   private static final IntFunction<NarratorStatus> BY_ID = ByIdMap.continuous(NarratorStatus::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<NarratorStatus> LEGACY_CODEC = Codec.INT.xmap(NarratorStatus::byId, NarratorStatus::getId);
   private final int id;
   private final Component name;

   NarratorStatus(final int id, final String key) {
      this.id = id;
      this.name = Component.translatable(key);
   }

   public int getId() {
      return this.id;
   }

   public Component getName() {
      return this.name;
   }

   public static NarratorStatus byId(final int id) {
      return BY_ID.apply(id);
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
