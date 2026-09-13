package net.minecraft.world.entity;

import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public record ConversionParams(ConversionType type, boolean keepEquipment, boolean preserveCanPickUpLoot, @Nullable PlayerTeam team) {
   public static ConversionParams single(Mob p_365634_, boolean p_367858_, boolean p_362735_) {
      return new ConversionParams(ConversionType.SINGLE, p_367858_, p_362735_, p_365634_.getTeam());
   }

   @FunctionalInterface
   public interface AfterConversion<T extends Mob> {
      void finalizeConversion(T var1);
   }
}
