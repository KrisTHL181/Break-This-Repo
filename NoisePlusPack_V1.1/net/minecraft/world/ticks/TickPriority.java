package net.minecraft.world.ticks;

import com.mojang.serialization.Codec;

public enum TickPriority {
   EXTREMELY_HIGH(-3),
   VERY_HIGH(-2),
   HIGH(-1),
   NORMAL(0),
   LOW(1),
   VERY_LOW(2),
   EXTREMELY_LOW(3);

   public static final Codec<TickPriority> CODEC = Codec.INT.xmap(TickPriority::byValue, TickPriority::getValue);
   private final int value;

   TickPriority(final int p_193444_) {
      this.value = p_193444_;
   }

   public static TickPriority byValue(int p_193447_) {
      for (TickPriority tickpriority : values()) {
         if (tickpriority.value == p_193447_) {
            return tickpriority;
         }
      }

      return p_193447_ < EXTREMELY_HIGH.value ? EXTREMELY_HIGH : EXTREMELY_LOW;
   }

   public int getValue() {
      return this.value;
   }
}
