package net.minecraft.world.flag;

import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

public interface FeatureElement {
   Set<ResourceKey<? extends Registry<? extends FeatureElement>>> FILTERED_REGISTRIES = Set.of(
      Registries.ITEM, Registries.BLOCK, Registries.ENTITY_TYPE, Registries.GAME_RULE, Registries.MENU, Registries.POTION, Registries.MOB_EFFECT
   );

   FeatureFlagSet requiredFeatures();

   default boolean isEnabled(FeatureFlagSet p_249172_) {
      return this.requiredFeatures().isSubsetOf(p_249172_);
   }
}
