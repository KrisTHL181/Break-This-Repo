package net.minecraft.world.effect;

import net.minecraft.ChatFormatting;

public enum MobEffectCategory {
   BENEFICIAL(ChatFormatting.BLUE),
   HARMFUL(ChatFormatting.RED),
   NEUTRAL(ChatFormatting.BLUE);

   private final ChatFormatting tooltipFormatting;

   MobEffectCategory(final ChatFormatting tooltipFormatting) {
      this.tooltipFormatting = tooltipFormatting;
   }

   public ChatFormatting getTooltipFormatting() {
      return this.tooltipFormatting;
   }
}
