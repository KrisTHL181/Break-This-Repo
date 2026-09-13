package net.minecraft.data.advancements.packs;

import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.advancements.AdvancementProvider;

public class VanillaAdvancementProvider {
   public static SingleRegistryBootstrap<Advancement> create() {
      return new AdvancementProvider(
         List.of(
            VanillaTheEndAdvancements::new,
            VanillaHusbandryAdvancements::new,
            VanillaAdventureAdvancements::new,
            VanillaNetherAdvancements::new,
            VanillaStoryAdvancements::new
         )
      );
   }
}
