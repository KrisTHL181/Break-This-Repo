package net.minecraft.world.item;

import com.google.common.collect.ImmutableBiMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopperBlocks;

public record WeatheringCopperItems(
   Item unaffected, Item exposed, Item weathered, Item oxidized, Item waxed, Item waxedExposed, Item waxedWeathered, Item waxedOxidized
) {
   public static WeatheringCopperItems create(WeatheringCopperBlocks p_430597_, Function<Block, Item> p_423128_) {
      return new WeatheringCopperItems(
         p_423128_.apply(p_430597_.unaffected()),
         p_423128_.apply(p_430597_.exposed()),
         p_423128_.apply(p_430597_.weathered()),
         p_423128_.apply(p_430597_.oxidized()),
         p_423128_.apply(p_430597_.waxed()),
         p_423128_.apply(p_430597_.waxedExposed()),
         p_423128_.apply(p_430597_.waxedWeathered()),
         p_423128_.apply(p_430597_.waxedOxidized())
      );
   }

   public ImmutableBiMap<Item, Item> waxedMapping() {
      return ImmutableBiMap.of(
         this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
      );
   }

   public void forEach(Consumer<Item> p_429717_) {
      p_429717_.accept(this.unaffected);
      p_429717_.accept(this.exposed);
      p_429717_.accept(this.weathered);
      p_429717_.accept(this.oxidized);
      p_429717_.accept(this.waxed);
      p_429717_.accept(this.waxedExposed);
      p_429717_.accept(this.waxedWeathered);
      p_429717_.accept(this.waxedOxidized);
   }
}
