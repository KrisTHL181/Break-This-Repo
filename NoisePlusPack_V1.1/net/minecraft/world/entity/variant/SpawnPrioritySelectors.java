package net.minecraft.world.entity.variant;

import com.mojang.serialization.Codec;
import java.util.List;

public record SpawnPrioritySelectors(List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors) {
   public static final SpawnPrioritySelectors EMPTY = new SpawnPrioritySelectors(List.of());
   public static final Codec<SpawnPrioritySelectors> CODEC = PriorityProvider.Selector.codec(SpawnCondition.CODEC)
      .listOf()
      .xmap(SpawnPrioritySelectors::new, SpawnPrioritySelectors::selectors);

   public static SpawnPrioritySelectors single(SpawnCondition p_395363_, int p_397597_) {
      return new SpawnPrioritySelectors(PriorityProvider.single(p_395363_, p_397597_));
   }

   public static SpawnPrioritySelectors fallback(int p_392663_) {
      return new SpawnPrioritySelectors(PriorityProvider.alwaysTrue(p_392663_));
   }
}
